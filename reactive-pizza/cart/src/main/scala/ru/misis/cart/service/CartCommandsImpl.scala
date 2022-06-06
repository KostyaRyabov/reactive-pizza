package ru.misis.cart.service

import akka.Done
import akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticDsl.{matchQuery, _}
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import com.sksamuel.elastic4s.requests.script.Script
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.{ElasticClient, RequestSuccess}
import ru.misis.cart.model.CartJsonFormats._
import ru.misis.cart.model.{CartCommands, CartSettings}
import ru.misis.elastic.Menu.menuHitReader
import ru.misis.event.Cart._
import ru.misis.event.EventJsonFormats.{orderConfirmedJsonFormat, orderFormedJsonFormat}
import ru.misis.event.Menu._
import ru.misis.event.{CartConfirmed, CartCreated, Mapper, Order}
import ru.misis.util.{WithElasticInit, WithKafka, WithLogger}

import scala.concurrent.{ExecutionContext, Future}

class CartCommandsImpl(val elastic: ElasticClient, val settings: CartSettings)
                      (implicit val executionContext: ExecutionContext, val system: ActorSystem)
  extends CartCommands
    with WithKafka
    with WithLogger
    with WithElasticInit {

  logger.info(s"Cart server initializing ...")

  initElastic("cart")(
    properties(
      keywordField("id"),
      keywordField("cartId"),
      textField("name").analyzer("russian"),
      doubleField("price"),
      intField("amount"),
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
    logger.info(s"Getting cart $cartId..")

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
    logger.info(s"Deletion cart $cartId..")

    elastic
      .execute(deleteByQuery("cart", boolQuery.should(matchQuery("cartId", cartId))))
      .map(_ => Done)
  }

  override def addItem(item: Item): Future[ItemData] = {
    logger.info(s"Adding item#${item.itemId} to cart#${item.cartId}..")

    if (item.amount <= 0) {
      Future.failed(throw new Exception("Quantity of item is non-positive!"))
    } else {
      getItemInfo(item) { itemInfo =>
        logger.info(s"Adding item to cart ${itemInfo.cartId}:${itemInfo.itemId}..")

        for {
          numOpt <- elastic.execute(search("cart").query(
            boolQuery.must(
              multiMatchQuery(item.itemId),
              multiMatchQuery(item.cartId),
            )
          )).map({ case results: RequestSuccess[SearchResponse] =>
            results.result.to[ItemData].headOption.map(_.amount)
          })
          result <- {
            numOpt match {
              case Some(num) =>
                updateItem(item.copy(amount = itemInfo.amount + num))
                  .map(_ => itemInfo.copy(amount = itemInfo.amount + num))
              case _ =>
                elastic.execute(
                  indexInto("cart")
                    .fields(
                      "id" -> itemInfo.itemId,
                      "cartId" -> itemInfo.cartId,
                    )
                    .doc(itemInfo)
                ).map({
                  case request: RequestSuccess[IndexResponse] => itemInfo
                })
            }
          }
        } yield {
          result
        }
      }
    }
  }

  override def deleteItem(
                           itemId: String,
                           cartId: String,
                         ): Future[Done] = {
    logger.info(s"Deletion item from cart $cartId:$itemId..")

    for {
      result <- elastic.execute(
        deleteByQuery("cart", boolQuery.must(
          multiMatchQuery(itemId),
          multiMatchQuery(cartId),
        ))
      ).map(_ => Done)
    } yield result
  }

  private def updateItem(item: Item): Future[Item] = {
    logger.info(s"Updating item on cart ${item.cartId}:${item.itemId}..")

    elastic.execute(
      updateByQuery("cart", boolQuery().must(
        multiMatchQuery(item.itemId),
        multiMatchQuery(item.cartId),
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
      result <- publishEvent(CartCreated(cartInfo))
    } yield result
  }

  override def confirmCart(cartId: String): Future[Done] = {
    logger.info(s"Confirming cart#$cartId..")

    for {
      cart <- getCart(cartId)
      data = Order(
        id = cart.id,
        items = cart.items.flatMap(Mapper.cartItem2OrderItems),
      )
      result <- publishEvent(CartConfirmed(data))
    } yield {
      result
    }
  }
}
