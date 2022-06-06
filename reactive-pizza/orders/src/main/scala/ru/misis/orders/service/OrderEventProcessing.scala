package ru.misis.orders.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import ru.misis.event.EventJsonFormats._
import ru.misis.event.{CartConfirmed, ItemStateUpdated}
import ru.misis.orders.model.OrderCommands
import ru.misis.orders.service.Waiter.{ReturnOrder, TakeOrder}
import ru.misis.util.{WithKafka, WithLogger}
import spray.json.enrichAny

import scala.concurrent.{ExecutionContext, Future}

case class IsNotReady(msg: String) extends Exception(msg)

class OrderEventProcessing(service: OrderCommands)
                          (implicit val system: ActorSystem, ec: ExecutionContext)
  extends WithKafka
    with WithLogger {

  logger.info("Order Event Processing Initializing ...")

  kafkaSource[CartConfirmed]
    .map(event => {
      logger.info(s"Cart confirmed! ${event.data.toJson.prettyPrint}")
      event
    })
    .map(order => service.waiter ! TakeOrder(order.data))
    .runWith(Sink.ignore)

  kafkaSource[ItemStateUpdated]
    .map(event => {
      logger.info(s"Item#${event.item.orderId}:${event.item.menuItemId} State Updated! ${event.item.state} [${event.item.isReady}]")
      event
    })
    .mapAsync(1)({
      case ItemStateUpdated(item) if item.isReady =>
        logger.info(s"ItemStateUpdated ${item.id} - READY!!!")
        Thread.sleep(1000)
        service.getOrder(item.orderId).map(orderOps =>
          {
            logger.info(s"orderOps ${orderOps.map(_.id)} - ${orderOps.map(_.getItemsSumState)}!!!")

            orderOps.filter(_.isReady).map(order =>
              service.waiter ! ReturnOrder(order)
            )
          }
        )
      case ItemStateUpdated(item) =>
        Future.successful(logger.info(s"Item#${item.orderId}:${item.menuItemId} is not ready!"))
    })
    .runWith(Sink.ignore)
}
