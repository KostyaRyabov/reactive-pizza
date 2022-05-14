package ru.misis.cart.service

import akka.actor.ActorSystem
import ru.misis.cart.model.CartCommands
import ru.misis.event.EventJsonFormats.paymentConfirmedFormat
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

  // todo: добавить потом событие в сервис заказов
  kafkaSource[PaymentConfirmed]
    .runWith({ case PaymentConfirmed(id) => logger.info("Payment of bill#{} is confirmed.", id) })
}
