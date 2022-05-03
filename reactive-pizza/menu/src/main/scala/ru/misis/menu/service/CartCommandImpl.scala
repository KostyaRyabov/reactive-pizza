package ru.misis.menu.service

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
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
import ru.misis.menu.service.MenuCommandImpl.itemIndex
import ru.misis.util.WithKafka

import scala.concurrent.{ExecutionContext, Future}
import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.{ElasticClient, RequestSuccess}
import ru.misis.event.Menu.MenuCreated
import ru.misis.menu.model.MenuCommands
import ru.misis.menu.model.ModelJsonFormats._
import ru.misis.menu.model.Objects._
import ru.misis.util.WithKafka
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import io.scalaland.chimney.dsl.TransformerOps
import org.slf4j.LoggerFactory
import ru.misis.event.Menu._
import ru.misis.menu.model.Objects._
import ru.misis.menu.model.MenuCommands
import ru.misis.util.WithKafka
import spray.json._
import ru.misis.event.EventJsonFormats._
import ru.misis.menu.model.ModelJsonFormats._
import spray.json.DefaultJsonProtocol.immIndexedSeqFormat

object CartCommandImpl {
  val menuIndex = "menu"

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

  elastic.execute {
    deleteIndex(menuIndex)
  }
    .flatMap { _ =>
      elastic.execute {
        createIndex(menuIndex).mapping(
          properties(
            nestedField("categories").properties(
              textField("name").boost(4).analyzer("russian"),
              nestedField("items").properties(
                keywordField("id"),
                textField("name").boost(4).analyzer("russian"),
                textField("description").boost(4).analyzer("russian"),
                doubleField("price")
              )
            )
          )
        )
      }
    }

  kafkaSource[MenuCreated]
    .wireTap(event => {
      elastic.execute {
        indexInto(menuIndex).doc(event.menu)
      }
        .map({
          case results: RequestSuccess[IndexResponse] =>
            logger.info(s"Menu created ${event.menu.toJson.prettyPrint}")
        })
    })
    .runWith(Sink.ignore)

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

  import ru.misis.event.EventJsonFormats._
  import spray.json._

  override def getMenu: Future[Menu] = {
    logger.info("Getting menu..")

    elastic
      .execute(search(menuIndex).query(matchAllQuery()))
      .map({ case results: RequestSuccess[SearchResponse] => results.result.to[Menu].head })
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

  private def getItemInfo[A](item: Cart.Item)(f: Cart.ItemData => Future[A]): Future[A] = {
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

  def addItem(item: Cart.Item): Future[Cart.ItemData] = {
    if (item.amount <= 0) {
      Future.failed(throw new Exception("Quantity of item is non-positive!"))
    } else {
      getItemInfo(item) { itemInfo =>
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
  }

  override def deleteItem(
                           itemId: String,
                           cartId: String,
                         ): Future[Done] = {
    logger.info(s"Deletion item from chart $cartId:$itemId..")

    for {
      result <- elastic.execute(
        deleteByQuery(cartIndex, boolQuery().should(
          matchQuery(itemIdField, itemId),
          matchQuery(cartIdField, cartId),
        ))
      ).map(_ => Done)
    } yield result
  }

  private def updateItem(item: Cart.Item): Future[Cart.Item] = {
    logger.info(s"Updating item on chart ${item.cartId}:${item.itemId}..")

    elastic.execute(
      updateByQuery(cartIndex, boolQuery().should(
        matchQuery(itemIdField, item.itemId),
        matchQuery(cartIdField, item.cartId),
      ))
        .script(
          Script("ctx._source.amount=params.amount;")
            .params(Map("amount" -> item.amount))
            .lang("painless")
        )
    ).map(_ => item)
  }

  override def putItem(item: Cart.Item): Future[Either[Done, Cart.Item]] = {
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
}
