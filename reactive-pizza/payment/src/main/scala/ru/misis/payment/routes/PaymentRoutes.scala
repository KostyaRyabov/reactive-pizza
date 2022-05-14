package ru.misis.payment.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ru.misis.payment.model.PaymentCommands
import ru.misis.payment.model.PaymentJsonFormats.billJsonFormat
import spray.json.DefaultJsonProtocol.immSeqFormat
import spray.json._

class PaymentRoutes(paymentService: PaymentCommands) {

  val routes: Route = {
    path("payment") {
      path("confirm" / Segment) { cartId =>
        get {
          onSuccess(paymentService.confirm(cartId)) { cartInfo =>
            complete((StatusCodes.OK, cartInfo))
          }
        }
      } ~
        path("list") {
          get {
            onSuccess(paymentService.getUnpaidOrders) { bills =>
              complete(bills.toJson.sortedPrint)
            }
          }
        }
    }
  }
}
