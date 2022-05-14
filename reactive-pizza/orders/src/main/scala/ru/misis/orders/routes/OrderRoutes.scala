package ru.misis.orders.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ru.misis.event.EventJsonFormats._
import ru.misis.event.Order
import ru.misis.orders.model.OrderCommands

class OrderRoutes(orderService: OrderCommands) {

  val routes: Route = {
    path("orders") {
      (post & entity(as[Order])) { order =>
        onSuccess(orderService.placeOrder(order)) { _ =>
          complete(StatusCodes.Created)
        }
      } ~
        path(Segment) { id =>
          get {
            onSuccess(orderService.getOrder(id)) { response =>
              complete((StatusCodes.OK, response))
            }
          } ~
            delete {
              onSuccess(orderService.completeOrder(id)) { _ =>
                complete(StatusCodes.OK)
              }
            } ~
            path("state") {
              get {
                onSuccess(orderService.getOrderState(id)) { state =>
                  complete((StatusCodes.OK, state.toString))
                }
              }
            }
        }
    }
  }
}
