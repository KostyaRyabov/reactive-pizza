package ru.misis.event

import java.util.UUID

import ru.misis.event.State._

object Order {
  val initStage: Int = -1

  case class Item(
                   menuItemId: String,
                   name: String,
                   state: State = State.InWait,
                 )
    extends WithState

  case class ItemData(
                       id: String = UUID.randomUUID().toString,
                       orderId: String,
                       override val menuItemId: String,
                       override val name: String,
                       override val state: State = State.InWait,
                     )
    extends Item(menuItemId, name, state)
      with WithState
}

case class CartConfirmed(data: Order) extends Event

case class OrderSubmitted(items: Seq[Order.ItemData]) extends Event

case class OrderCompleted(order: Order) extends Event

case class ItemStateUpdated(item: Order.ItemData) extends Event

case class OrderReturned(order: Order) extends Event

case class OrderData(id: String, items: Seq[Order.ItemData]) extends WithItemsState

case class Order(id: String, items: Seq[Order.Item]) extends WithItemsState
