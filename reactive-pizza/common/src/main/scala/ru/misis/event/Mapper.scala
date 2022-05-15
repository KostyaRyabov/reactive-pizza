package ru.misis.event

import ru.misis.event.State.State

object Mapper {
  def itemData2ItemInfo(data: Cart.ItemData): Cart.ItemInfo = {
    Cart.ItemInfo(
      id = data.itemId,
      name = data.name,
      price = data.price,
      amount = data.amount,
    )
  }

  def cartItem2OrderItem(data: Cart.ItemInfo): Order.Item = {
    Order.Item(
      menuItemId = data.id,
      name = data.name,
      state = State.InWait,
    )
  }

  def orderItemMinimize(item: Order.ItemData): Order.Item = {
    Order.Item(
      menuItemId = item.menuItemId,
      name = item.name,
      state = item.state,
    )
  }

  def orderItemRefine(orderId: String)(item: Order.Item): Order.ItemData = {
    Order.ItemData(
      orderId = orderId,
      menuItemId = item.menuItemId,
      name = item.name,
      state = item.state,
    )
  }

  def orderDataMinimize(data: OrderData): Order = {
    Order(
      id = data.id,
      items = data.items.map(Mapper.orderItemMinimize),
    )
  }
}
