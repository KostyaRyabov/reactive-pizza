package ru.misis.elastic

import com.sksamuel.elastic4s.{HitReader, Indexable}
import ru.misis.event.EventJsonFormats._
import ru.misis.event.Menu.RouteItem
import spray.json._

import scala.util.Try

object RouteCard {
  implicit val menuHitReader: HitReader[RouteItem] = hit => Try(hit.sourceAsString.parseJson.convertTo[RouteItem])
  implicit val menuIndexable: Indexable[RouteItem] = item => item.toJson.compactPrint
}
