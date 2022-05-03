package ru.misis.menu.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import io.scalaland.chimney.dsl.TransformerOps
import org.slf4j.LoggerFactory
import ru.misis.event.Menu._
import ru.misis.menu.model.Objects._
import ru.misis.menu.model.MenuCommands
import ru.misis.util.WithKafka
import spray.json._

import scala.concurrent.ExecutionContext

class MenuEventProcessing (menuService: MenuCommands)
                          (implicit executionContext: ExecutionContext,
                           override val system: ActorSystem)
    extends WithKafka {

    import ru.misis.event.EventJsonFormats._
    import ru.misis.menu.model.ModelJsonFormats._

    private val logger = LoggerFactory.getLogger(this.getClass)

    kafkaSource[ItemsEvent]
        .map{ event =>
            Menu(event.items
                .groupBy(_.category)
                .map{ case (name, items) =>
                    MenuCategory(name, items.map(item => item.into[MenuItem].transform))
                }
                .toSeq
            ) -> event.items.map(item => RouteItem(item.id, item.routeStages))
        }
        .mapAsync(1){ case (menu, routeCard) =>
            for {
                _ <- publishEvent(MenuCreated(menu))
                result <- publishEvent(RouteCardCreated(routeCard))
            } yield result
        }
        .runWith(Sink.ignore)

    kafkaSource[RouteCardCreated]
        .wireTap(value => logger.info(s"RouteCard created ${value.toJson.prettyPrint}"))
        .runWith(Sink.ignore)
}
