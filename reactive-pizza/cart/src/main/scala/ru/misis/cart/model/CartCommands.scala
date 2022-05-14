package ru.misis.cart.model

import akka.Done
import ru.misis.event.Cart
import ru.misis.event.Cart.{CartInfo, Item, ItemData}
import ru.misis.event.Menu.Menu

import scala.concurrent.Future

trait CartCommands {

  def getCart(
               cartId: String,
             ): Future[CartInfo]

  def deleteCart(
                  cartId: String,
                ): Future[Done]

  def addItem(item: Item): Future[ItemData]

  def deleteItem(
                  itemId: String,
                  cartId: String,
                ): Future[Done]

  def putItem(item: Item): Future[Either[Done, Item]]

  def pay(
           cartId: String,
         ): Future[Done]
}
