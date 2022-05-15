package ru.misis.kitchen.service

import akka.actor.ActorSystem
import ru.misis.kitchen.model.KitchenCommands
import ru.misis.util.{WithKafka, WithLogger}

class KitchenEventProcessing(paymentService: KitchenCommands)
                            (implicit override val system: ActorSystem)
  extends WithKafka
    with WithLogger {

  logger.info("Kitchen Event Processing Initializing ...")

  ???

}
