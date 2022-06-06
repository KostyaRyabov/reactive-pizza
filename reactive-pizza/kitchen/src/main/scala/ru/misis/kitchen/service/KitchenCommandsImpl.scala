package ru.misis.kitchen.service

import akka.Done
import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.{ActorRef, DispatcherSelector, SupervisorStrategy}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.script.Script
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.update.UpdateByQueryResponse
import com.sksamuel.elastic4s.{ElasticClient, RequestSuccess}
import ru.misis.elastic.RouteCard._
import ru.misis.event.EventJsonFormats.itemStateUpdatedJsonFormat
import ru.misis.event.Menu.RouteItem
import ru.misis.event.Order.ItemData
import ru.misis.event.{ItemStateUpdated, Order}
import ru.misis.kitchen.model.{KitchenCommands, KitchenSettings}
import ru.misis.util.{WithElasticInit, WithKafka, WithLogger}

import scala.concurrent.{ExecutionContext, Future}

class KitchenCommandsImpl(val elastic: ElasticClient, val settings: KitchenSettings)
                         (implicit val executionContext: ExecutionContext, val system: ActorSystem)
  extends KitchenCommands
    with WithKafka
    with WithLogger
    with WithElasticInit {

  logger.info(s"Kitchen server initializing...")

  private val chefPool = Routers.pool(poolSize = settings.chefInstancesNum) {
    Behaviors.supervise(Chef(this)).onFailure[Exception](SupervisorStrategy.restart)
  }
    .withRoundRobinRouting()
    .withRouteeProps(routeeProps = DispatcherSelector.blocking())

  override val chef: ActorRef[Chef.Command] = system.spawn(chefPool, "chef-pool")

  initElastic("route_card")(
    properties(
      keywordField("itemId"),
      nestedField("routeStages").properties(
        textField("name").boost(4).analyzer("russian"),
        textField("description").analyzer("russian"),
        nestedField("products").properties(
          textField("name"),
          intField("amount"),
        ),
        intField("duration"),
      )
    )
  )

  override def saveRouteCard(routeCard: Seq[RouteItem]): Future[Seq[RouteItem]] = {
    logger.info(s"Route Card updating...")

    for {
      _ <- elastic.execute(deleteByQuery("route_card", matchAllQuery()))
      _ <- elastic.execute(bulk(routeCard.map(indexInto("route_card").doc(_))))
    } yield {
      logger.info(s"Route Card is created!")
      routeCard
    }
  }

  override def getRouteCard: Future[Seq[RouteItem]] = {
    elastic.execute(search("route_card").query(matchAllQuery()))
      .map({ case results: RequestSuccess[SearchResponse] => results.result.to[RouteItem] })
  }

  override def getRouteItem(menuItemId: String): Future[RouteItem] = {
    logger.info(s"Getting routeItem#$menuItemId ...")

    elastic.execute(search("route_card").query(matchQuery("itemId", menuItemId)))
      .map({ case results: RequestSuccess[SearchResponse] => results.result.to[RouteItem].head })
  }

  private def updateItemState(item: ItemData): Future[ItemData] = {
    logger.info(s"Updating item#${item.orderId}:${item.menuItemId} state to '${item.state}' ...")

    elastic.execute(
      updateByQuery("orders", boolQuery().must(matchQuery("id", item.id)))
        .script(
          Script("ctx._source.state=params.state;")
            .params(Map("state" -> item.state))
            .lang("painless")
        )
    ).map({
      case response: RequestSuccess[UpdateByQueryResponse] => item
      case ex => throw new Exception(s"Error submitting! ${ex.toString}")
    })
  }

  override def submit(item: Order.ItemData): Future[Done] = {
    logger.info(s"Submit item#${item.id} [${item.state}]")

    updateItemState(item)
      .flatMap(_ => publishEvent(ItemStateUpdated(item)))
  }
}
