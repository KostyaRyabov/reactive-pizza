package ru.misis.cart.model

import com.sksamuel.elastic4s.{HitReader, Indexable}
import ru.misis.event.Cart.ItemData
import ru.misis.event.EventJsonFormats._
import spray.json._

import scala.util.Try


object CartJsonFormats {
  implicit val cartHitReader: HitReader[ItemData] = hit => Try(hit.sourceAsString.parseJson.convertTo[ItemData])
  implicit val cartIndexable: Indexable[ItemData] = itemInfo => itemInfo.toJson.compactPrint
}
