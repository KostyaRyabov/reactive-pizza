package ru.misis.cart.service

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.script.Script
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.{ElasticClient, RequestSuccess}
import ru.misis.cart.model.CartJsonFormats._
import ru.misis.cart.model.CartCommands
import ru.misis.elastic.Menu.menuHitReader
import ru.misis.event.Cart._
import ru.misis.event.EventJsonFormats.orderFormedFormat
import ru.misis.event.{Mapper, Order}
import ru.misis.event.Menu._
import ru.misis.event.Order.{OrderRequest, OrderConfirmed, OrderFormed}
import ru.misis.util.{WithKafka, WithLogger}

import scala.concurrent.{ExecutionContext, Future}

class CartCommandsImpl(
                       elastic: ElasticClient,
                     )(implicit
                       executionContext: ExecutionContext,
                       val system: ActorSystem,
                     )
  extends CartCommands
    with WithKafka
    with WithLogger {

  logger.info(s"Cart server initializing ...")

  elastic.execute(
    deleteIndex("cart")
  )
    .flatMap(_ =>
      elastic.execute(
        createIndex("cart").mapping(
          properties(
            keywordField("id"),
            keywordField("cartId"),
            textField("name").analyzer("russian"),
            doubleField("price"),
            intField("amount"),
          )
        )
      )
    )

  private def getMenu: Future[Menu] = {
    logger.info("Getting menu..")

    elastic
      .execute(search("menu").matchAllQuery())
      .map({ case results: RequestSuccess[SearchResponse] => results.result.to[Menu].head })
  }

  private def getMenuItems: Future[Set[MenuItem]] = {
    getMenu.map(_.categories.flatMap(_.items).toSet)
  }

  private def refine(item: Item)(menuItems: Set[MenuItem]): Option[ItemData] = {
    menuItems
      .find(_.id == item.itemId)
      .map(menuItem =>
        ItemData(
          itemId = menuItem.id,
          cartId = item.cartId,
          name = menuItem.name,
          price = menuItem.price,
          amount = item.amount,
        )
      )
  }

  private def getItemInfo[A](item: Item)(f: ItemData => Future[A]): Future[A] = {
    for {
      itemInfoOpt <- getMenuItems
        .map(refine(item))
      itemInfo = itemInfoOpt.getOrElse(throw new ClassNotFoundException("Menu item not found!"))
      result <- f(itemInfo)
    } yield result
  }

  override def getCart(
                        cartId: String,
                      ): Future[CartInfo] = {
    logger.info(s"Getting chart $cartId..")

    elastic
      .execute(search("cart").query(matchQuery("cartId", cartId)))
      .map({ case results: RequestSuccess[SearchResponse] =>
        CartInfo(
          id = cartId,
          items = results.result.to[ItemData].map(Mapper.itemData2ItemInfo),
        )
      })
  }

  override def deleteCart(
                           cartId: String,
                         ): Future[Done] = {
    logger.info(s"Deletion chart $cartId..")

    elastic
      .execute(deleteByQuery("cart", matchQuery("cartId", cartId)))
      .map(_ => Done)
  }

  override def addItem(item: Item): Future[ItemData] = {
    if (item.amount <= 0) {
      Future.failed(throw new Exception("Quantity of item is non-positive!"))
    } else {
      getItemInfo(item) { itemInfo =>
        logger.info(s"Adding item to chart ${itemInfo.cartId}:${itemInfo.itemId}..")

        elastic.execute(
          indexInto("cart")
            .fields(
              "id" -> itemInfo.itemId,
              "cartId" -> itemInfo.cartId,
            )
            .doc(itemInfo)
        ).map(_ => itemInfo)
      }
    }
  }

  override def deleteItem(
                           itemId: String,
                           cartId: String,
                         ): Future[Done] = {
    logger.info(s"Deletion item from chart $cartId:$itemId..")

    for {
      result <- elastic.execute(
        deleteByQuery("cart", boolQuery().should(
          matchQuery("id", itemId),
          matchQuery("cartId", cartId),
        ))
      ).map(_ => Done)
    } yield result
  }

  private def updateItem(item: Item): Future[Item] = {
    logger.info(s"Updating item on chart ${item.cartId}:${item.itemId}..")

    elastic.execute(
      updateByQuery("cart", boolQuery().should(
        matchQuery("id", item.itemId),
        matchQuery("cartId", item.cartId),
      ))
        .script(
          Script("ctx._source.amount=params.amount;")
            .params(Map("amount" -> item.amount))
            .lang("painless")
        )
    ).map(_ => item)
  }

  override def putItem(item: Item): Future[Either[Done, Item]] = {
    if (item.amount <= 0) {
      deleteItem(
        itemId = item.itemId,
        cartId = item.cartId,
      ).map(Left(_))
    } else {
      updateItem(item)
        .map(Right(_))
    }
  }

  override def pay(
                    cartId: String,
                  ): Future[Done] = {
    logger.info(s"Pay order#$cartId..")

    for {
      cartInfo <- getCart(cartId)
      result <- Source.single(OrderFormed(cartInfo))
        .runWith(kafkaSink[OrderFormed])
    } yield result
  }

  override def prepareOrder(cartId: String): Future[Done] = {
    for {
      cart <- getCart(cartId)
      data = Order(
        id = cart.id,
        items = cart.items.map(Mapper.cartItem2OrderItem(cart.id)),
      )
    } yield {
      publishEvent[OrderConfirmed](OrderConfirmed(data))
    }
  }
}
