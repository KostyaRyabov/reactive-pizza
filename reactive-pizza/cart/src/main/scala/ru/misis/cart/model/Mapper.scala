package ru.misis.cart.model

import ru.misis.event.Cart

object Mapper {
  def itemData2ItemInfo(data: Cart.ItemData): Cart.ItemInfo = {
    Cart.ItemInfo(
      id = data.itemId,
      name = data.name,
      price = data.price,
      amount = data.amount,
    )
  }
}
