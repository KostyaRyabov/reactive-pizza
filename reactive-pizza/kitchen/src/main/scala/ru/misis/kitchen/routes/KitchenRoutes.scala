package ru.misis.kitchen.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ru.misis.event.EventJsonFormats.routeItemJsonFormat
import ru.misis.kitchen.model.KitchenCommands
import spray.json.DefaultJsonProtocol.immSeqFormat
import spray.json.enrichAny

class KitchenRoutes(service: KitchenCommands) {

  val routes: Route = {
    path("route" / Segment) { itemId =>
      get {
        onSuccess(service.getRouteItem(itemId)) { response =>
          complete((StatusCodes.OK, response.toJson.prettyPrint))
        }
      }
    } ~
      path("routes") {
        get {
          onSuccess(service.getRouteCard) { response =>
            complete((StatusCodes.OK, response.toJson.sortedPrint))
          }
        }
      }
  }
}
