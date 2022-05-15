package ru.misis.kitchen.model

import ru.misis.event.State._

import scala.concurrent.Future

trait KitchenCommands {
  def makeStep(itemId: String, orderId: String): Future[State]

  def getState(itemId: String, orderId: String): Future[State]
}
