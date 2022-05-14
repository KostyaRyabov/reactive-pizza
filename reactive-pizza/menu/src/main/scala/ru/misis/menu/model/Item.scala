package ru.misis.menu.model

import java.util.UUID

import ru.misis.event.Menu._

case class Item(
                 id: ItemId = UUID.randomUUID().toString,
                 name: String,
                 category: String,
                 description: Option[String],
                 price: Double,
                 routeStages: Seq[RouteStage]
               )


