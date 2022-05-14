package ru.misis.orders.service

import akka.Done
import akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticApi.{createIndex, deleteIndex, properties}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.{ElasticClient, RequestSuccess}
import ru.misis.event.Order._
import ru.misis.event.{Order, State, States}
import ru.misis.orders.model.OrderCommands
import ru.misis.orders.model.OrderJsonFormats._
import ru.misis.util.{WithKafka, WithLogger}

import scala.concurrent.{ExecutionContext, Future}

class OrderCommandsImpl(
                         elastic: ElasticClient,
                       )(implicit
                         executionContext: ExecutionContext,
                         val system: ActorSystem,
                       )
  extends OrderCommands
    with WithKafka
    with WithLogger {

  logger.info(s"Order server initializing ...")

  elastic.execute(
    deleteIndex("orders")
  )
    .flatMap(_ =>
      elastic.execute(
        createIndex("orders").mapping(
          properties(
            keywordField("id"),
            textField("orderId"),
            textField("menuItemId"),
            textField("name"),
            shortField("state"),
          )
        )
      )
    )

  private def getItems(orderId: String): Future[Seq[Item]] = {
    elastic.execute(search("orders").matchQuery("cartId", orderId))
      .map({ case results: RequestSuccess[SearchResponse] => results.result.to[Item] })
  }

  override def getOrder(orderId: String): Future[Order] = {
    getItems(orderId).map(items => Order(orderId, items))
  }

  override def placeOrder(order: Order): Future[Done] = {
    elastic.execute {
      bulk(order.items.map(indexInto("orders").doc))
    }.map(_ => Done)
  }

  override def getOrderState(orderId: String): Future[State] = {
    getItems(orderId).map {
      case items if items.exists(Item.isInProcess) => States.InProcess
      case items if items.forall(Item.isReady) => States.Ready
      case items if items.nonEmpty => States.InWait
      case _ => States.NotFound
    }
  }

  override def completeOrder(orderId: String): Future[Done] = {
    elastic.execute {
      deleteByQuery("orders", matchQuery("orderId", orderId))
    }.map(_ => Done)
  }
}
