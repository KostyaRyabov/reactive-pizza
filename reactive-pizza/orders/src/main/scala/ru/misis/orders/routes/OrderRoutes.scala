package ru.misis.orders.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ru.misis.event.EventJsonFormats._
import ru.misis.orders.model.OrderCommands
import spray.json.DefaultJsonProtocol.immSeqFormat
import spray.json.enrichAny

class OrderRoutes(orderService: OrderCommands) {

  val routes: Route = {
    path("orders") {
      get {
        onSuccess(orderService.getOrders) { response =>
          complete((StatusCodes.OK, response.toJson.sortedPrint))
        }
      }
    } ~
      path("order" / Segment) { id =>
        get {
          onSuccess(orderService.getOrder(id)) { response =>
            complete((StatusCodes.OK, response))
          }
        }
      } ~
      path("state" / Segment) { id =>
        get {
          onSuccess(orderService.getOrderState(id)) { state =>
            complete(state)
          }
        }
      }
  }
}
