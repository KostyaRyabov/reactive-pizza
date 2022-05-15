package ru.misis.menu.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.scalaland.chimney.dsl._
import ru.misis.event.EventJsonFormats._
import ru.misis.event.Menu.{Product, RouteStage}
import ru.misis.menu.model.ItemJsonFormats._
import ru.misis.menu.model.{Item, MenuCommands}
import spray.json.DefaultJsonProtocol._
import spray.json._

class MenuRoutes(menuService: MenuCommands) {

  implicit val itemDTOJsonFormat: RootJsonFormat[ItemDTO] = jsonFormat5(ItemDTO)

  val routes: Route =
    pathPrefix("menu") {
      get {
        onSuccess(menuService.getMenu) { menu =>
          complete((StatusCodes.OK, menu))
        }
      }
    } ~
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


case class ItemDTO(
                    name: String,
                    description: Option[String],
                    category: String,
                    price: Double,
                    routeStages: Seq[RouteStage],
                  )
