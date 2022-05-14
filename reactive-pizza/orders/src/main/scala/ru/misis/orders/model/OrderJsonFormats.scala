package ru.misis.orders.model

import com.sksamuel.elastic4s.{HitReader, Indexable}
import ru.misis.event.EventJsonFormats._
import ru.misis.event.Order.Item
import spray.json._

import scala.util.Try

object OrderJsonFormats {
  implicit val itemHitReader: HitReader[Item] = hit => Try(hit.sourceAsString.parseJson.convertTo[Item])
  implicit val itemIndexable: Indexable[Item] = item => item.toJson.compactPrint
}
