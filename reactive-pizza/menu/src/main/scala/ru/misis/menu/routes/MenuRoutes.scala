package ru.misis.menu.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ru.misis.menu.model.Objects._
import spray.json.DefaultJsonProtocol._
import io.scalaland.chimney.dsl._
import ru.misis.event.Menu.RouteStage
import ru.misis.menu.model.MenuCommands
import ru.misis.event.EventJsonFormats._
import ru.misis.menu.model.ModelJsonFormats._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json._

class MenuRoutes(menuService: MenuCommands)(implicit val system: ActorSystem){

    implicit val itemDTOJsonFormat = jsonFormat5(ItemDTO)

    val routes: Route =
    path("items") {
        get {
            onSuccess(menuService.listItems()) { items =>
                complete(items.toJson.sortedPrint)
            }
        }
    } ~
    path("item") {
        (post & entity(as[ItemDTO])) { itemDTO =>
            val item = itemDTO.into[Item]
                .transform

            onSuccess(menuService.saveItem(item)) { performed =>
                complete(StatusCodes.Created, performed)
            }
        }
    } ~
    path("item" / Segment) { id =>
        get {
            rejectEmptyResponse {
                onSuccess(menuService.getItem(id)) { response =>
                    complete(response)
                }
            }
        }
    } ~
    path("find" / Segment) { name =>
        get {
            onSuccess(menuService.findItem(name)) { items =>
                complete((StatusCodes.OK, items))
            }
        }
    } ~
    path("publish") {
        (post & entity(as[Seq[String]])) { itemIds =>
            onSuccess(menuService.publish(itemIds)) { _ =>
                complete(StatusCodes.OK)
            }
        }
    }
}


case class ItemDTO(name: String,
                   description: Option[String],
                   category: String,
                   price: Double,
                   routeStages: Seq[RouteStage])
