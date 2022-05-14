package ru.misis.event

object Payment {
  case class PaymentConfirmed(id: String) extends Event
}
