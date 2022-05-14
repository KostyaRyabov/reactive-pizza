package ru.misis.event

import java.util.UUID

import ru.misis.event.Cart.CartInfo

object Order {

  object Item {
    def isReady: Item => Boolean = _.state == States.Ready.value
    def isInProcess: Item => Boolean = _.state == States.InProcess.value
    def isInWait: Item => Boolean = _.state == States.InWait.value
    def isNotFound: Item => Boolean = _.state == States.NotFound.value
  }

  case class Item(
                   id: String = UUID.randomUUID().toString,
                   orderId: String,
                   menuItemId: String,
                   name: String,
                   state: Short,
                 )

  case class OrderFormed(cart: CartInfo) extends Event

  case class OrderConfirmed(data: Order) extends Event
}

case class Order(id: String, items: Seq[Order.Item])
