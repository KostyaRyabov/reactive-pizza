package ru.misis.kitchen.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ru.misis.kitchen.model.KitchenCommands

class KitchenRoutes(service: KitchenCommands) {

  val routes: Route = {
    path("kitchen") {
      pathPrefix("order" / Segment) { orderId =>
        pathPrefix("item" / Segment) { itemId =>
          get {
            onSuccess(service.getState(itemId, orderId)) { response =>
              complete((StatusCodes.OK, response))
            }
          }
        }
      }
    }
  }
}
