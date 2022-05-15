package ru.misis.orders.service

import akka.Done
import akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticApi.{createIndex, deleteIndex, properties}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.{ElasticClient, RequestSuccess}
import ru.misis.elastic.Order._
import ru.misis.event.Order._
import ru.misis.event.State.State
import ru.misis.event.{Mapper, Order, State}
import ru.misis.orders.model.OrderCommands
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
            doubleField("progress"),
          )
        )
      )
    )

  private def getItems(orderId: String): Future[Seq[ItemData]] = {
    elastic.execute(search("orders").matchQuery("cartId", orderId))
      .map({ case results: RequestSuccess[SearchResponse] => results.result.to[ItemData] })
  }

  override def getOrder(orderId: String): Future[Order] = {
    getItems(orderId).map(items => Order(orderId, items.map(Mapper.orderItemMinimize)))
  }

  override def placeOrder(order: Order): Future[Done] = {
    elastic.execute {
      bulk(
        order.items
          .map(Mapper.orderItemRefine(order.id))
          .map(indexInto("orders").doc)
      )
    }.map(_ => Done)
  }

  override def getOrderState(orderId: String): Future[State] = {
    getItems(orderId).map {
      case items if items.exists(_.isInProcess) => State.InProcess
      case items if items.forall(_.isReady) => State.Ready
      case items if items.forall(_.isInWait) => State.InWait
      case _ => State.NotFound
    }
  }

  override def completeOrder(orderId: String): Future[Done] = {
    elastic.execute {
      deleteByQuery("orders", matchQuery("orderId", orderId))
    }.map(_ => Done)
  }
}
