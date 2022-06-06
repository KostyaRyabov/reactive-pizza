package ru.misis.kitchen.model

import akka.Done
import akka.actor.typed.ActorRef
import ru.misis.event.Menu.RouteItem
import ru.misis.event.Order
import ru.misis.event.State._
import ru.misis.kitchen.service.Chef

import scala.concurrent.Future

trait KitchenCommands {
  val chef: ActorRef[Chef.Command]

  def getRouteItem(itemId: String): Future[RouteItem]

  def getRouteCard: Future[Seq[RouteItem]]

  def saveRouteCard(routeCard: Seq[RouteItem]): Future[Seq[RouteItem]]

  def submit(item: Order.ItemData): Future[Done]
}
