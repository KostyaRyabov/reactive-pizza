package ru.misis.orders.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import ru.misis.event.EventJsonFormats._
import ru.misis.event.Order.OrderConfirmed
import ru.misis.orders.model.OrderCommands
import ru.misis.util.{WithKafka, WithLogger}


class OrderEventProcessing(orderService: OrderCommands)
                          (implicit override val system: ActorSystem)
  extends WithKafka
    with WithLogger {

  logger.info("Order Event Processing Initializing ...")

  kafkaSource[OrderConfirmed]
    .wireTap(order => orderService.placeOrder(order.data))
    .runWith(Sink.ignore)
}
