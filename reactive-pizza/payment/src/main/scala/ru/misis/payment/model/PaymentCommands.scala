package ru.misis.payment.model

import akka.Done
import ru.misis.event.CartCreated

import scala.concurrent.Future

trait PaymentCommands {
  def confirm: CartCreated => Future[Done]

  def getList: Future[Seq[Payment]]

  def getPayment(id: String): Future[Payment]
}
