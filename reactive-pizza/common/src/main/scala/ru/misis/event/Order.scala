package ru.misis.event

import java.util.UUID

import ru.misis.event.Cart.CartInfo
import ru.misis.event.State._

trait ItemLike extends WithState {
  def menuItemId: String

  def name: String

  def state: State

  def setState(state: State): ItemLike
}


object Order {
  val initStage: Int = -1

  case class Item(
                   menuItemId: String,
                   name: String,
                   state: State = State.InWait,
                 )
    extends ItemLike {

    override def setState(state: State): ItemLike = copy(state = state)
  }

  case class ItemData(
                       id: String = UUID.randomUUID().toString,
                       orderId: String,
                       menuItemId: String,
                       name: String,
                       state: State = State.InWait,
                       stage: Int = initStage,
                     )
    extends ItemLike {

    def setStage(stage: Int): ItemData = copy(stage = stage)

    def makeStage: ItemData = copy(
      state = State.InProcess,
      stage = stage + 1,
    )

    override def setState(state: State): ItemLike = copy(
      state = state,
      stage = state match {
        case State.InProcess => stage
        case _ => initStage
      }
    )
  }


  case class OrderFormed(cart: CartInfo) extends Event

  case class OrderConfirmed(data: Order) extends Event
}

case class OrderData(id: String, items: Seq[Order.ItemData])

case class Order(id: String, items: Seq[Order.Item])
