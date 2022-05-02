package ru.misis.menu.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import org.slf4j.LoggerFactory
import ru.misis.event.Cart._
import ru.misis.menu.model.CartCommands
import ru.misis.util.WithKafka
import spray.json._

import scala.concurrent.ExecutionContext

class CartEventProcessing(menuService: CartCommands)
                         (implicit executionContext: ExecutionContext,
                          override val system: ActorSystem)
  extends WithKafka {

  import ru.misis.event.EventJsonFormats._

  private val logger = LoggerFactory.getLogger(this.getClass)

  kafkaSource[OrderFormed]
    .wireTap(value => logger.info(s"Order formed ${value.toJson.prettyPrint}"))
    .runWith(Sink.ignore)

}
