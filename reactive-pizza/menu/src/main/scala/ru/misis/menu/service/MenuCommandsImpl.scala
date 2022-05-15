package ru.misis.menu.service

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.{ElasticClient, RequestSuccess}
import io.scalaland.chimney.dsl.TransformerOps
import ru.misis.elastic.Menu._
import ru.misis.elastic.RouteCart._
import ru.misis.event.Menu._
import ru.misis.menu.model.ItemJsonFormats._
import ru.misis.menu.model.{Item, ItemsEvent, MenuCommands}
import ru.misis.util.{WithKafka, WithLogger}

import scala.concurrent.{ExecutionContext, Future}

class MenuCommandsImpl(elastic: ElasticClient)
                      (implicit executionContext: ExecutionContext,
                       val system: ActorSystem)
  extends MenuCommands
    with WithKafka
    with WithLogger {

  logger.info(s"Menu server initializing ...")

  elastic.execute {
    deleteIndex("menu")
  }
    .flatMap { _ =>
      elastic.execute {
        createIndex("menu").mapping(
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

  elastic.execute {
    deleteIndex("item")
  }
    .flatMap { _ =>
      elastic.execute {
        createIndex("item").mapping(
          properties(
            keywordField("id"),
            textField("name").boost(4).analyzer("russian"),
            textField("category"),
            textField("description").analyzer("russian"),
            doubleField("price"),
            nestedField("routeCard").properties(
              textField("name"),
              textField("description"),
              intField("duration"),
              nestedField("products").properties(
                textField("name"),
                intField("amount")
              )
            )
          )
        )
      }
    }

  elastic.execute {
    deleteIndex("routeCart")
  }
    .flatMap { _ =>
      elastic.execute {
        createIndex("routeCart").mapping(
          properties(
            keywordField("itemId"),
            nestedField("routeStages").properties(
              textField("name").boost(4).analyzer("russian"),
              textField("description").analyzer("russian"),
              nestedField("products").properties(
                textField("name"),
                intField("amount"),
              ),
              intField("duration"),
            )
          )
        )
      }
    }

  override def getMenu: Future[Menu] = {
    logger.info("Getting menu..")

    elastic
      .execute(search("menu").matchAllQuery())
      .map({ case results: RequestSuccess[SearchResponse] => results.result.to[Menu].head })
  }

  override def listItems(): Future[Seq[Item]] = {
    elastic.execute(search("item"))
      .map {
        case results: RequestSuccess[SearchResponse] => results.result.to[Item]
      }

  }

  override def getItem(id: String): Future[Item] =
    elastic.execute(get("item", id))
      .map {
        case results: RequestSuccess[GetResponse] => results.result.to[Item]
      }

  override def findItem(name: String): Future[Seq[Item]] = {
    elastic.execute {
      search("item").query(
        should(
          matchQuery("name", name),
          matchQuery("description", name)
        )
      )
    }
      .map {
        case results: RequestSuccess[SearchResponse] => results.result.to[Item]
      }
  }

  override def saveItem(item: Item): Future[Item] =
    elastic.execute {
      indexInto("item").id(item.id).doc(item)
    }
      .map {
        case results: RequestSuccess[IndexResponse] => item
      }

  override def publish(itemIds: Seq[String]): Future[Done] = {
    for {
      items <- Future.sequence(itemIds.map { itemId =>
        elastic.execute(get("item", itemId))
          .map {
            case results: RequestSuccess[GetResponse] => results.result.to[Item]
          }
      })
      result <- Source.single(ItemsEvent(items))
        .runWith(kafkaSink[ItemsEvent])
    } yield result
  }

  private def saveMenu(menu: Menu): Future[Menu] = {
    logger.info(s"Menu updating...")

    elastic.execute {
      indexInto("menu").doc(menu)
    }
      .map({
        case results: RequestSuccess[IndexResponse] => menu
      })
  }

  private def saveRouteCart(routeCard: Seq[RouteItem]): Future[Seq[RouteItem]] = {
    logger.info(s"Route Cart updating...")

    for {
      _ <- elastic.execute {
        deleteByQuery("routeCart", matchAllQuery())
      }
      _ <- elastic.execute {
        bulk(routeCard.map(indexInto("routeCart").doc))
      }
    } yield routeCard
  }

  override def createMenu(items: Seq[Item]): Future[MenuCreated] = {
    logger.info(s"Menu creation...")

    val categories = items
      .groupBy(_.category)
      .map({
        case (name, items) =>
          MenuCategory(name, items.map(item => item.into[MenuItem].transform))
      })
      .toSeq

    for {
      menu <- saveMenu(Menu(categories))
    } yield MenuCreated(menu)
  }

  override def createRouteMap(items: Seq[Item]): Future[RouteCardCreated] = {
    logger.info(s"Route Map creation...")

    val routeCard = items.map(item => RouteItem(item.id, item.routeStages))
    saveRouteCart(routeCard).map(RouteCardCreated)
  }
}
