package ru.misis.menu.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink}
import ru.misis.event.EventJsonFormats._
import ru.misis.event.Menu.{MenuCreated, RouteCardCreated}
import ru.misis.menu.model.ItemJsonFormats._
import ru.misis.menu.model.{ItemsEvent, MenuCommands}
import ru.misis.util.{StreamHelper, WithKafka, WithLogger}
import spray.json.enrichAny

class MenuEventProcessing(menuService: MenuCommands)
                         (implicit val system: ActorSystem)
  extends WithKafka
    with WithLogger
    with StreamHelper {

  logger.info("Menu Event Processing Initializing ...")

  kafkaSource[ItemsEvent]
    .runWith(broadcastSink2(
      Flow[ItemsEvent]
        .mapAsync(1)({ case ItemsEvent(items) => menuService.createMenu(items) })
        .to(kafkaSink),
      Flow[ItemsEvent]
        .map({ case ItemsEvent(items) => menuService.createRouteMap(items) })
        .to(kafkaSink)
    ))

  kafkaSource[MenuCreated]
    .wireTap(value => logger.info("Menu created: {}", value.toJson.prettyPrint))
    .runWith(Sink.ignore)

  kafkaSource[RouteCardCreated]
    .wireTap(value => logger.info("RouteCard created: {}", value.toJson.prettyPrint))
    .runWith(Sink.ignore)
}
