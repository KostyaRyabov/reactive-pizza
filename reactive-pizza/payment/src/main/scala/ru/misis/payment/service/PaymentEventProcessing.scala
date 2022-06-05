package ru.misis.payment.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import ru.misis.event.CartCreated
import ru.misis.event.EventJsonFormats.orderFormedJsonFormat
import ru.misis.payment.model.PaymentCommands
import ru.misis.util.{WithKafka, WithLogger}

class PaymentEventProcessing(paymentService: PaymentCommands)
                            (implicit override val system: ActorSystem)
  extends WithKafka
    with WithLogger {

  logger.info("Payment Event Processing Initializing ...")

  kafkaSource[CartCreated]
    .mapAsync(1)(paymentService.confirm(_))
    .to(Sink.ignore)

}
