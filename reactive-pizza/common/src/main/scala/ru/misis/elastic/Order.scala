package ru.misis.elastic

import com.sksamuel.elastic4s.{HitReader, Indexable}
import ru.misis.event.EventJsonFormats._
import ru.misis.event.Order.ItemData
import spray.json._

import scala.util.Try

object Order {
  implicit val itemHitReader: HitReader[ItemData] = hit => Try(hit.sourceAsString.parseJson.convertTo[ItemData])
  implicit val itemIndexable: Indexable[ItemData] = item => item.toJson.compactPrint
}
