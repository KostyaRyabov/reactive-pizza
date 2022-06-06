package ru.misis.orders.service

import akka.Done
import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.{ActorRef, DispatcherSelector, SupervisorStrategy}
import com.sksamuel.elastic4s.ElasticApi.properties
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.script.Script
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.update.UpdateByQueryResponse
import com.sksamuel.elastic4s.{ElasticClient, RequestSuccess}
import ru.misis.elastic.Order._
import ru.misis.event.EventJsonFormats.orderSubmittedJsonFormat
import ru.misis.event.Mapper.orderItemRefine
import ru.misis.event.Order._
import ru.misis.event.State.State
import ru.misis.event.{Mapper, Order, OrderData, OrderSubmitted, State}
import ru.misis.orders.model.{OrderCommands, OrderSettings}
import ru.misis.util.{WithElasticInit, WithKafka, WithLogger}

import scala.concurrent.{ExecutionContext, Future}

class OrderCommandsImpl(val elastic: ElasticClient, val settings: OrderSettings)
                       (implicit val executionContext: ExecutionContext, val system: ActorSystem)
  extends OrderCommands
    with WithKafka
    with WithLogger
    with WithElasticInit {

  logger.info(s"Order server initializing ...")

  private val waiterPool = Routers.pool(poolSize = settings.waiterInstancesNum) {
    Behaviors.supervise(Waiter(this)).onFailure[Exception](SupervisorStrategy.restart)
  }
    .withRoundRobinRouting()
    .withRouteeProps(routeeProps = DispatcherSelector.blocking())

  override val waiter: ActorRef[Waiter.Command] = system.spawn(waiterPool, "waiter-pool")

  initElastic("orders")(
    properties(
      keywordField("id"),
      textField("orderId"),
      textField("menuItemId"),
      textField("name"),
      textField("state"),
    )
  )

  private def getItems(orderId: String): Future[Seq[ItemData]] = {
    elastic.execute(search("orders").query(matchQuery("orderId", orderId)))
      .map({ case results: RequestSuccess[SearchResponse] => results.result.to[ItemData] })
  }

  override def getOrders: Future[Seq[OrderData]] = {
    for {
      items <- elastic.execute(search("orders").matchAllQuery())
        .map({ case results: RequestSuccess[SearchResponse] => results.result.to[ItemData] })
      groups = items.groupBy(_.orderId)
    } yield {
      groups.map(group => OrderData(
        id = group._1,
        items = group._2,
      )).toSeq
    }
  }

  override def getOrder(orderId: String): Future[Option[Order]] = {
    logger.info(s"Getting order#$orderId ...")
    for {
      items <- getItems(orderId)
        .map(_.map(Mapper.orderItemMinimize))
    } yield {
      Option(Order(orderId, items))
        .filter(_.items.nonEmpty)
    }
  }

  override def getOrderState(orderId: String): Future[State] = {
    for {
      order <- getOrder(orderId)
    } yield {
      order.map(_.getItemsSumState)
        .getOrElse(State.NotFound)
    }
  }

  override def submit(order: Order): Future[Done] = {
    logger.info(s"Submitting order#${order.id} [${order.items.length}]...")
    val items = order.items.map(orderItemRefine(order.id))
    for {
      _ <- elastic.execute(bulk(items.map(item => indexInto("orders").doc(item))))
      _ <- publishEvent(OrderSubmitted(items))
    } yield {
      logger.info(s"Order#${order.id} submitted!")
      Done
    }
  }

  override def returnOrder(order: Order): Future[Done] = {
    elastic.execute(
      updateByQuery("orders", matchQuery("orderId", order.id))
        .script(
          Script("ctx._source.state=params.state;")
            .params(Map("state" -> State.Completed))
            .lang("painless")
        )
    ).map({ case response: RequestSuccess[UpdateByQueryResponse] =>
      logger.info(s"Order#${order.id} returned!")
      Done
    })
  }
}
