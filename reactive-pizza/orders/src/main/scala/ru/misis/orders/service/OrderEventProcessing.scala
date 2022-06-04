package ru.misis.orders.service

import akka.actor.ActorSystem
import akka.stream.{ActorAttributes, Supervision}
import akka.stream.scaladsl.Sink
import ru.misis.event.EventJsonFormats._
import ru.misis.event.{CartConfirmed, ItemStateUpdated, State}
import ru.misis.orders.model.OrderCommands
import ru.misis.orders.service.Waiter.{ReturnOrder, TakeOrder}
import ru.misis.util.{WithKafka, WithLogger}

import scala.concurrent.ExecutionContext

class OrderEventProcessing(service: OrderCommands)
                          (implicit override val system: ActorSystem, ec: ExecutionContext)
  extends WithKafka
    with WithLogger {

  logger.info("Order Event Processing Initializing ...")

//  val decider: Supervision.Decider = {
//    case _: NoSuchElementException => Supervision.Resume
//    case _ => Supervision.Stop
//  }

  kafkaSource[CartConfirmed]
    .wireTap(_ => logger.info("Cart confirmed!"))
    .map(order => service.waiter ! TakeOrder(order.data))
    .runWith(Sink.ignore)

  kafkaSource[ItemStateUpdated]
//    .withAttributes(ActorAttributes.supervisionStrategy(decider))
    .mapAsync(1)({ case ItemStateUpdated(item) if item.isReady =>
      service.getOrder(item.orderId)
        .filter(order => order.isReady)
        .map(order => service.waiter ! ReturnOrder(order))
    })
    .runWith(Sink.ignore)
}
