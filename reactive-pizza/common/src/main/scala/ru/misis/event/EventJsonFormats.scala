package ru.misis.event

import ru.misis.event.Cart._
import ru.misis.event.Menu._
import ru.misis.event.Payment._
import spray.json.DefaultJsonProtocol._

object EventJsonFormats {
  implicit val productJsonFormat = jsonFormat2(Product)
  implicit val routeStageJsonFormat = jsonFormat4(RouteStage)
  implicit val routeItemJsonFormat = jsonFormat2(RouteItem)
  implicit val menuItemJsonFormat = jsonFormat4(MenuItem)

  implicit val menuCategoryJsonFormat = jsonFormat2(MenuCategory)
  implicit val menuJsonFormat = jsonFormat1(Menu)

  implicit val menuCreatedJsonFormat = jsonFormat1(MenuCreated)
  implicit val routeCardCreatedJsonFormat = jsonFormat1(RouteCardCreated)

  implicit val cartItemDTOJsonFormat = jsonFormat2(ItemDTO)
  implicit val cartAppendItemJsonFormat = jsonFormat3(Item)
  implicit val cartItemDataJsonFormat = jsonFormat5(ItemData)

  implicit val cartItemInfoJsonFormat = jsonFormat4(ItemInfo)
  implicit val cartInfoJsonFormat = jsonFormat2(CartInfo)

  implicit val paymentConfirmedJsonFormat = jsonFormat1(PaymentConfirmed)

  implicit val orderItemDataJsonFormat = jsonFormat5(Order.ItemData)
  implicit val orderItemJsonFormat = jsonFormat4(Order.Item.apply)
  implicit val orderDataJsonFormat = jsonFormat2(OrderData)
  implicit val orderJsonFormat = jsonFormat2(Order.apply)

  implicit val orderFormedJsonFormat = jsonFormat1(CartCreated)
  implicit val orderConfirmedJsonFormat = jsonFormat1(CartConfirmed)

  implicit val itemStateUpdatedJsonFormat = jsonFormat1(ItemStateUpdated)

  implicit val orderSubmittedJsonFormat = jsonFormat1(OrderSubmitted)
}
