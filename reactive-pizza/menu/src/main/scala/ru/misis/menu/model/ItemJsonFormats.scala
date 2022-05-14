package ru.misis.menu.model

import com.sksamuel.elastic4s.{HitReader, Indexable}
import ru.misis.event.EventJsonFormats._
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.util.Try

object ItemJsonFormats {
    implicit val itemJsonFormat: RootJsonFormat[Item] = jsonFormat6(Item)
    implicit val itemsEventFormat: RootJsonFormat[ItemsEvent] = jsonFormat1(ItemsEvent)

    implicit val itemHitReader: HitReader[Item] = hit => Try(hit.sourceAsString.parseJson.convertTo[Item])
    implicit val itemIndexable: Indexable[Item] = item => item.toJson.compactPrint
}
