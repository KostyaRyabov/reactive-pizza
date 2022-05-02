package ru.misis.event

import java.util.UUID

object Cart {

  // for requests:

  case class Item(
                   itemId: String,
                   cartId: String = UUID.randomUUID().toString,
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

  case class OrderFormed(cart: CartInfo) extends Event

}
