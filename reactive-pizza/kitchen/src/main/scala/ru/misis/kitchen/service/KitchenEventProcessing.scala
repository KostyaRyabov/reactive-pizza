package ru.misis.kitchen.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import ru.misis.event.EventJsonFormats.routeCardCreatedJsonFormat
import ru.misis.event.Menu.RouteCardCreated
import ru.misis.event.OrderSubmitted
import ru.misis.kitchen.model.KitchenCommands
import ru.misis.event.EventJsonFormats._
import ru.misis.kitchen.service.Chef.TakeItem
import ru.misis.util.{WithKafka, WithLogger}

class KitchenEventProcessing(service: KitchenCommands)
                            (implicit val system: ActorSystem)
  extends WithKafka
    with WithLogger {

  logger.info("Kitchen Event Processing Initializing ...")

  kafkaSource[OrderSubmitted]
    .wireTap(order => logger.info(s"Order#${order.items.headOption.map(_.id).getOrElse("?")} is submitted!"))
    .map(_.items.map(item => service.chef ! TakeItem(item)))
    .runWith(Sink.ignore)

  kafkaSource[RouteCardCreated]
    .mapAsync(1)({ case RouteCardCreated(routeCard) =>
      service.saveRouteCard(routeCard)
    })
    .runWith(Sink.ignore)

}
