package ru.misis.event

import ru.misis.event.Menu._
import spray.json.DefaultJsonProtocol._

object EventJsonFormats {
    implicit val productJsonFormat = jsonFormat2(Product)
    implicit val stageJsonFormat = jsonFormat4(RouteStage)
    implicit val stageItemJsonFormat = jsonFormat2(RouteItem)
    implicit val menuItemJsonFormat = jsonFormat4(MenuItem)

    implicit val menuCategoryFormat = jsonFormat2(MenuCategory)
    implicit val menuFormat = jsonFormat1(Menu)

    implicit val menuCreatedFormat = jsonFormat1(MenuCreated)
    implicit val routeCardCreatedFormat = jsonFormat1(RouteCardCreated)

    implicit val cartItemFormat = jsonFormat3(Cart.Item)
    implicit val cartItemDataFormat = jsonFormat5(Cart.ItemData)

    implicit val cartItemInfoFormat = jsonFormat4(Cart.ItemInfo)
    implicit val cartInfoFormat = jsonFormat2(Cart.CartInfo)
    implicit val orderFormedFormat = jsonFormat1(Cart.OrderFormed)
}
