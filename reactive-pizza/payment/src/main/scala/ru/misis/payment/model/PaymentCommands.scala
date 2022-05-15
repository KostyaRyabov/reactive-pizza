package ru.misis.payment.model

import akka.Done
import ru.misis.event.OrderData.OrderFormed

import scala.concurrent.Future

trait PaymentCommands {
  def confirm(id: String): Future[Done]

  def create: OrderFormed => Future[Bill]

  def getUnpaidOrders: Future[Seq[Bill]]
}
