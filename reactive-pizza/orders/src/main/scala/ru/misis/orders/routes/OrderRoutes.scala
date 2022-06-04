package ru.misis.orders.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ru.misis.event.EventJsonFormats._
import ru.misis.orders.model.OrderCommands

class OrderRoutes(orderService: OrderCommands) {

  val routes: Route = {
    path("orders") {
      pathPrefix(Segment) { id =>
        get {
          onSuccess(orderService.getOrder(id)) { response =>
            complete((StatusCodes.OK, response))
          }
        } ~
          pathPrefix("state") {
            get {
              onSuccess(orderService.getOrderState(id)) { state =>
                complete(state)
              }
            }
          }
      }
    }
  }
}
