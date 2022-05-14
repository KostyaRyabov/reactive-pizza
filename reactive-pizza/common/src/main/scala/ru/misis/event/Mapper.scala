package ru.misis.event

import ru.misis.event.StateImpl.toShort

object Mapper {
  def itemData2ItemInfo(data: Cart.ItemData): Cart.ItemInfo = {
    Cart.ItemInfo(
      id = data.itemId,
      name = data.name,
      price = data.price,
      amount = data.amount,
    )
  }

  def cartItem2OrderItem(orderId: String)(data: Cart.ItemInfo): Order.Item = {
    Order.Item(
      orderId = orderId,
      menuItemId = data.id,
      name = data.name,
      state = States.InWait,
    )
  }
}
