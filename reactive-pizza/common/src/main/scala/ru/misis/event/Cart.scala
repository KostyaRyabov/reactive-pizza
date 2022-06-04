package ru.misis.event

import java.util.UUID

import ru.misis.event.Cart.CartInfo

object Cart {

  // for requests:

  case class Item(
                   itemId: String,
                   cartId: String = UUID.randomUUID().toString,
                   amount: Int,
                 )

  case class ItemDTO(
                      itemId: String,
                      amount: Int,
                    )

  // for storing:

  case class ItemData(
                       itemId: String,
                       cartId: String = UUID.randomUUID().toString,
                       name: String,
                       price: Double,
                       amount: Int,
                     )

  // for output:

  case class ItemInfo(
                       id: String,
                       name: String,
                       price: Double,
                       amount: Int,
                     )

  case class CartInfo(
                       id: String,
                       items: Seq[ItemInfo],
                     )
}

case class CartCreated(cart: CartInfo) extends Event
