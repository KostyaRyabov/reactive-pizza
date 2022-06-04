package ru.misis.payment.model

import java.time.LocalDateTime

import com.sksamuel.elastic4s.{HitReader, Indexable}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.util.Try

object PaymentJsonFormats {
  implicit val paymentJsonFormat: RootJsonFormat[Payment] = jsonFormat3(Payment)

  implicit val billHitReader: HitReader[Payment] = hit => Try(hit.sourceAsString.parseJson.convertTo[Payment])
  implicit val billIndexable: Indexable[Payment] = bill => bill.toJson.compactPrint
}
