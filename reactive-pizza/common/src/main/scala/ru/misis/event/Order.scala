package ru.misis.event

import java.util.UUID

import ru.misis.event.State._

object Order {
  val initStage: Int = -1

  trait ItemLike extends WithState {
    def menuItemId: String
    def name: String
    def state: State
  }

  case class Item(
                   id: String = UUID.randomUUID().toString,
                   menuItemId: String,
                   name: String,
                   state: State = State.InWait,
                 )
    extends ItemLike

  case class ItemData(
                       id: String,
                       orderId: String,
                       menuItemId: String,
                       name: String,
                       state: State = State.InWait,
                     )
    extends ItemLike
}

case class CartConfirmed(data: Order) extends Event

case class OrderSubmitted(items: Seq[Order.ItemData]) extends Event

case class OrderCompleted(order: Order) extends Event

case class ItemStateUpdated(item: Order.ItemData) extends Event

case class OrderReturned(order: Order) extends Event

case class OrderData(id: String, items: Seq[Order.ItemData]) extends WithItemsState

case class Order(id: String, items: Seq[Order.Item]) extends WithItemsState
