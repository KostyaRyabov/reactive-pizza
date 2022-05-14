package ru.misis.cart.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import ru.misis.cart.model.CartCommands
import ru.misis.event.EventJsonFormats.paymentConfirmedFormat
import ru.misis.event.Mapper
import ru.misis.event.Order.OrderConfirmed
import ru.misis.event.Payment.PaymentConfirmed
import ru.misis.util.{StreamHelper, WithKafka, WithLogger}

import scala.concurrent.ExecutionContext

class CartEventProcessing(cartService: CartCommands)
                         (implicit executionContext: ExecutionContext,
                          override val system: ActorSystem)
  extends WithKafka
    with WithLogger
    with StreamHelper {

  logger.info("Cart Event Processing Initializing ...")

  kafkaSource[PaymentConfirmed]
    .wireTap(payment => cartService.prepareOrder(payment.id))
    .runWith(Sink.ignore)
}
