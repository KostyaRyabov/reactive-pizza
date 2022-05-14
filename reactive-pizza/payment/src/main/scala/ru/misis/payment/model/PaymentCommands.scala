package ru.misis.payment.model

import akka.Done

import scala.concurrent.Future

trait PaymentCommands {
  def confirm(id: String): Future[Done]

  def getUnpaidOrders: Future[Seq[Bill]]
}
