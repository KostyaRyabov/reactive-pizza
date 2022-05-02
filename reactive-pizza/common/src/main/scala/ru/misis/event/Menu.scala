package ru.misis.event

import spray.json._

object Menu {
    type ItemId = String

    case class Menu(categories: Seq[MenuCategory])

    case class MenuCategory(name: String,
                            items: Seq[MenuItem])

    case class MenuItem(id: ItemId,
                        name: String,
                        description: Option[String],
                        price: Double)

    case class RouteItem(itemId: ItemId, routeStages: Seq[RouteStage])

    case class RouteStage(name: String,
                          description: Option[String],
                          products: Seq[Product],
                          duration: Int)

    case class Product(name: String, amount: Double)


    case class MenuCreated(menu: Menu) extends Event

    case class RouteCardCreated(routeCard: Seq[RouteItem]) extends Event
}
