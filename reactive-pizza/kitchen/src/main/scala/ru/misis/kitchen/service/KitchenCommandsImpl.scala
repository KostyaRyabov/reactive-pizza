package ru.misis.kitchen.service

import akka.Done
import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.{ActorRef, SupervisorStrategy}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.update.UpdateResponse
import com.sksamuel.elastic4s.{ElasticClient, RequestSuccess}
import ru.misis.elastic.Order._
import ru.misis.elastic.RouteCard._
import ru.misis.event.EventJsonFormats.itemStateUpdatedJsonFormat
import ru.misis.event.Menu.RouteItem
import ru.misis.event.Order.ItemData
import ru.misis.event.State.State
import ru.misis.event.{ItemStateUpdated, Order}
import ru.misis.kitchen.model.{KitchenCommands, KitchenConfig}
import ru.misis.util.{WithElasticInit, WithKafka, WithLogger}

import scala.concurrent.{ExecutionContext, Future}

class KitchenCommandsImpl(val elastic: ElasticClient, config: KitchenConfig)
                         (implicit val executionContext: ExecutionContext, val system: ActorSystem)
  extends KitchenCommands
    with WithKafka
    with WithLogger
    with WithElasticInit {

  logger.info(s"Kitchen server initializing...")

  private val chefPool = Routers.pool(poolSize = 3) {
    Behaviors.supervise(Chef(this)).onFailure[Exception](SupervisorStrategy.restart)
  }
  override val chef: ActorRef[Chef.Command] = system.spawn(chefPool, "chef-pool")

  initElastic("routeCard")(
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
      _ <- elastic.execute {
        deleteByQuery("routeCard", matchAllQuery())
      }
      _ <- elastic.execute {
        bulk(routeCard.map(indexInto("routeCard").doc))
      }
    } yield routeCard
  }

  override def getRouteItem(itemId: String): Future[RouteItem] = {
    elastic.execute {
      search("routeCard").matchQuery("itemId", itemId)
    }
      .map {
        case results: RequestSuccess[SearchResponse] => results.result.to[RouteItem].head
      }
  }

  private def updateItemState(item: ItemData): Future[ItemData] = {
    elastic.execute(updateById("orders", item.id).doc(item))
      .map({ case response: RequestSuccess[UpdateResponse] => item })
  }

  private def getOrderItem(itemId: String, orderId: String): Future[ItemData] = {
    elastic.execute(search("orders").query(
      must(
        matchQuery("orderId", orderId),
        matchQuery("menuItemId", itemId),
      )
    ))
      .map({ case results: RequestSuccess[SearchResponse] => results.result.to[ItemData].head })
  }

  override def getState(itemId: String, orderId: String): Future[State] = {
    getOrderItem(itemId, orderId).map(_.state)
  }

  override def submit(item: Order.ItemData): Future[Done] = {
    updateItemState(item)
      .flatMap(_ => publishEvent(ItemStateUpdated(item)))
  }
}
