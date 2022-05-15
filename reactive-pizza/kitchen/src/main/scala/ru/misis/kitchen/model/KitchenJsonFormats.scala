package ru.misis.kitchen.model

import com.sksamuel.elastic4s.{HitReader, Indexable}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.util.Try

object KitchenJsonFormats {
//  implicit val billJsonFormat: RootJsonFormat[Bill] = jsonFormat2(Bill)
//
//  implicit val billHitReader: HitReader[Bill] = hit => Try(hit.sourceAsString.parseJson.convertTo[Bill])
//  implicit val billIndexable: Indexable[Bill] = bill => bill.toJson.compactPrint
}
