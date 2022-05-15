package ru.misis.menu.model

import akka.Done
import ru.misis.event.Menu.{Menu, MenuCreated, RouteCardCreated}

import scala.concurrent.Future

trait MenuCommands {
  def getMenu: Future[Menu]

  def listItems(): Future[Seq[Item]]

  def getItem(id: String): Future[Item]

  def findItem(query: String): Future[Seq[Item]]

  def saveItem(item: Item): Future[Item]

  def publish(itemIds: Seq[String]): Future[Done]

  def createMenu(items: Seq[Item]): Future[MenuCreated]

  def createRouteMap(items: Seq[Item]): Future[RouteCardCreated]
}
