package ru.misis.orders.model

import akka.Done
import ru.misis.event.{Order, State}

import scala.concurrent.Future

trait OrderCommands {
  def getOrder(orderId: String): Future[Order]

  def placeOrder(order: Order): Future[Done]

  def getOrderState(orderId: String): Future[State]

  def completeOrder(orderId: String): Future[Done]
}
