package ru.misis.orders.model

import akka.Done
import akka.actor.typed.ActorRef
import ru.misis.event.{Order, OrderData}
import ru.misis.event.State.State
import ru.misis.orders.service.Waiter

import scala.concurrent.Future

trait OrderCommands {
  val waiter: ActorRef[Waiter.Command]

  def getOrder(orderId: String): Future[Order]

  def getOrderState(orderId: String): Future[State]

  def submit(order: Order): Future[Done]

  def returnOrder(order: Order): Future[Done]
}
