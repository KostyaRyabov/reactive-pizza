package ru.misis.cart.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import ru.misis.cart.model.CartCommands
import ru.misis.event.EventJsonFormats.paymentConfirmedJsonFormat
import ru.misis.event.Payment.PaymentConfirmed
import ru.misis.util.{StreamHelper, WithKafka, WithLogger}

import scala.concurrent.ExecutionContext

class CartEventProcessing(cartService: CartCommands)
                         (implicit executionContext: ExecutionContext, val system: ActorSystem)
  extends WithKafka
    with WithLogger
    with StreamHelper {

  logger.info("Cart Event Processing Initializing ...")

  kafkaSource[PaymentConfirmed]
    .wireTap(_ => logger.info("Payment confirmed!"))
    .mapAsync(1)(payment => cartService.confirmCart(payment.id))
    .runWith(Sink.ignore)
}
