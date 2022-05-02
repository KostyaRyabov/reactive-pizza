package ru.misis.menu.service

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.script.Script
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.{ElasticClient, RequestSuccess}
import org.slf4j.LoggerFactory
import ru.misis.event.Cart
import ru.misis.event.Cart._
import ru.misis.event.EventJsonFormats._
import ru.misis.event.Menu._
import ru.misis.menu.model.CartJsonFormats._
import ru.misis.menu.model.{CartCommands, Mapper}
import ru.misis.util.WithKafka

import scala.concurrent.{ExecutionContext, Future}

object CartCommandImpl {
  val cartIndex = "cart"

  val itemIdField = "id"
  val cartIdField = "cartId"
  val itemNameField = "name"
  val itemPriceField = "price"
  val itemAmountField = "amount"
}

class CartCommandImpl(
                       elastic: ElasticClient,
                     )(implicit
                       executionContext: ExecutionContext,
                       val system: ActorSystem,
                     )
  extends CartCommands
    with WithKafka {

  val logger = LoggerFactory.getLogger(this.getClass)
  logger.info(s"Cart server initializing ...")

  import CartCommandImpl._


  elastic.execute(
    deleteIndex(cartIndex)
  )
    .flatMap(_ =>
      elastic.execute(
        createIndex(cartIndex).mapping(
          properties(
            keywordField(itemIdField),
            keywordField(cartIdField),
            textField(itemNameField).analyzer("russian"),
            doubleField(itemPriceField),
            intField(itemAmountField),
          )
        )
      )
    )

  override def getMenu: Future[Menu] = {
    kafkaSource[MenuCreated]
      .runWith(Sink.last)
      .map(_.menu)
  }

  private def getMenuItems: Future[Set[MenuItem]] = {
    getMenu.map(_.categories.flatMap(_.items).toSet)
  }

  private def refine(item: Cart.Item)(menuItems: Set[MenuItem]): Option[Cart.ItemData] = {
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

  private def getItemInfo[A](f: Cart.ItemData => Future[A])(item: Cart.Item): Future[A] = {
    for {
      itemInfoOpt <- getMenuItems
        .map(refine(item))
      itemInfo = itemInfoOpt.getOrElse(throw new ClassNotFoundException("Menu item not found!"))
      result <- f(itemInfo)
    } yield result
  }

  override def getCart(
                        cartId: String,
                      ): Future[Cart.CartInfo] = {
    logger.info(s"Getting chart $cartId..")

    elastic
      .execute(search(cartIndex).query(matchQuery(cartIdField, cartId)))
      .map({ case results: RequestSuccess[SearchResponse] =>
        Cart.CartInfo(
          id = cartId,
          items = results.result.to[Cart.ItemData].map(Mapper.itemData2ItemInfo),
        )
      })
  }

  override def deleteCart(
                           cartId: String,
                         ): Future[Done] = {
    logger.info(s"Deletion chart $cartId..")

    elastic
      .execute(deleteByQuery(cartIndex, matchQuery(cartIdField, cartId)))
      .map(_ => Done)
  }

  def addItem: Cart.Item => Future[Cart.ItemData] = {
    getItemInfo { itemInfo =>
      logger.info(s"Adding item to chart ${itemInfo.cartId}:${itemInfo.itemId}..")

      elastic.execute(
        indexInto(cartIndex)
          .fields(
            itemIdField -> itemInfo.itemId,
            cartIdField -> itemInfo.cartId,
          )
          .doc(itemInfo)
      ).map(_ => itemInfo)
    }
  }

  override def deleteItem(
                           itemId: String,
                           cartId: String,
                         ): Future[Done] = {
    logger.info(s"Deletion item from chart $cartId:$itemId..")

    for {
      result <- elastic.execute(
        deleteByQuery(cartIndex, must(
          matchQuery(itemIdField, itemId),
          matchQuery(cartIdField, cartId),
        ))
      ).map(_ => Done)
    } yield result
  }

  private def updateItem(item: Cart.Item): Future[Cart.Item] = {
    logger.info(s"Updating item on chart ${item.cartId}:${item.itemId}..")

    elastic.execute(
      updateByQuery(cartIndex, must(
        matchQuery(itemIdField, item.itemId),
        matchQuery(cartIdField, item.cartId),
      ))
        .script(Script(s"ctx._source.amount = ${item.amount}"))
    ).map(_ => item)
  }

  override def putItem(item: Cart.Item): Future[Either[Done, Cart.Item]] = {
    if (item.amount == 0) {
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
}
