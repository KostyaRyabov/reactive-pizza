package ru.misis.payment.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ru.misis.payment.model.PaymentCommands
import ru.misis.payment.model.PaymentJsonFormats.paymentJsonFormat
import spray.json.DefaultJsonProtocol.immSeqFormat
import spray.json._

class PaymentRoutes(paymentService: PaymentCommands) {

  val routes: Route = {
    path("payments") {
      get {
        onSuccess(paymentService.getList) { payments =>
          complete(payments.toJson.sortedPrint)
        }
      }
    } ~
      path("payment" / Segment) { id =>
        get {
          onSuccess(paymentService.getPayment(id)) { payment =>
            complete(payment.toJson.prettyPrint)
          }
        }
      }
  }
}
