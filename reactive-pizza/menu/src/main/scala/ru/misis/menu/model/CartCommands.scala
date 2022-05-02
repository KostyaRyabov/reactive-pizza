package ru.misis.menu.model

import akka.Done
import ru.misis.event.Cart._
import ru.misis.event.Menu.Menu

import scala.concurrent.Future

trait CartCommands {

  def getMenu: Future[Menu]

  def getCart(
               cartId: String,
             ): Future[CartInfo]

  def deleteCart(
                  cartId: String,
                ): Future[Done]

  def addItem: Item => Future[ItemData]

  def deleteItem(
                  itemId: String,
                  cartId: String,
                ): Future[Done]

  def putItem(item: Item): Future[Either[Done, Item]]

  def pay(
           cartId: String,
         ): Future[Done]
}
