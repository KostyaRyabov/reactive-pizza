package ru.misis.kitchen.service

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticApi.{createIndex, deleteIndex, properties}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.update.UpdateResponse
import com.sksamuel.elastic4s.{ElasticClient, RequestSuccess}
import ru.misis.elastic.Order._
import ru.misis.elastic.RouteCart._
import ru.misis.event.Menu.RouteItem
import ru.misis.event.Order.ItemData
import ru.misis.event.State.State
import ru.misis.kitchen.model.KitchenCommands
import ru.misis.util.{WithKafka, WithLogger}

import scala.concurrent.{ExecutionContext, Future}

class KitchenCommandsImpl(
                           elastic: ElasticClient,
                         )(implicit
                           executionContext: ExecutionContext,
                           val system: ActorSystem,
                         )
  extends KitchenCommands
    with WithKafka
    with WithLogger {

  logger.info(s"Kitchen server initializing...")

  elastic.execute {
    deleteIndex("kitchen")
  }
    .flatMap { _ =>
      elastic.execute {
        createIndex("kitchen").mapping(
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
      }
    }

  private def getRouteItem(itemId: String): Future[RouteItem] = {
    elastic.execute {
      search("routeCart").matchQuery("itemId", itemId)
    }
      .map {
        case results: RequestSuccess[SearchResponse] => results.result.to[RouteItem].head
      }
  }

  private def updateOrderItem(data: ItemData): Future[ItemData] = {
    elastic.execute(updateById("orders", data.id).doc(data) )
      .map({ case response: RequestSuccess[UpdateResponse] => data })
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

  override def makeStep(itemId: String, orderId: String): Future[State] = {
    for {
      item <- getOrderItem(itemId, orderId)
      route <- getRouteItem(itemId)
      stages = route.routeStages
      updatedItem <- {
        if (item.stage <= stages.size) updateOrderItem(item.makeStage)
        else Future.successful(item)
      }
    } yield updatedItem.state
  }

  override def getState(itemId: String, orderId: String): Future[State] = {
    getOrderItem(itemId, orderId).map(_.state)
  }
}
