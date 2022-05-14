package ru.misis.elastic

import com.sksamuel.elastic4s.{HitReader, Indexable}
import ru.misis.event.EventJsonFormats._
import ru.misis.event.Menu.Menu
import spray.json._

import scala.util.Try

object Menu {
  implicit val menuHitReader: HitReader[Menu] = hit => Try(hit.sourceAsString.parseJson.convertTo[Menu])
  implicit val menuIndexable: Indexable[Menu] = menu => menu.toJson.compactPrint
}
