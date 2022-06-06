package ru.misis.menu.service

import akka.Done
import akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.{ElasticClient, RequestSuccess}
import io.scalaland.chimney.dsl.TransformerOps
import ru.misis.elastic.Menu._
import ru.misis.event.Menu._
import ru.misis.menu.model.ItemJsonFormats._
import ru.misis.menu.model.{Item, ItemsEvent, MenuCommands, MenuSettings}
import ru.misis.util.{WithElasticInit, WithKafka, WithLogger}

import scala.concurrent.{ExecutionContext, Future}

class MenuCommandsImpl(val elastic: ElasticClient, val settings: MenuSettings)
                      (implicit val executionContext: ExecutionContext, val system: ActorSystem)
  extends MenuCommands
    with WithKafka
    with WithLogger
    with WithElasticInit {

  logger.info(s"Menu server initializing ...")

  initElastic("menu")(
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

  initElastic("item")(
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

  override def getMenu: Future[Menu] = {
    logger.info("Getting menu..")

    elastic
      .execute(search("menu").matchAllQuery())
      .map({
        case results: RequestSuccess[SearchResponse] => results.result.to[Menu].head
      })
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
      items <- Future.sequence(itemIds.map {
        itemId =>
          elastic.execute(get("item", itemId))
            .map {
              case results: RequestSuccess[GetResponse] => results.result.to[Item]
            }
      })
      result <- publishEvent(ItemsEvent(items))
    } yield result
  }

  private def saveMenu(menu: Menu): Future[Menu] = {
    logger.info(s"Menu updating...")

    for {
      _ <- elastic.execute(deleteByQuery("menu", matchAllQuery()))
      _ <- elastic.execute(indexInto("menu").doc(menu))
    } yield {
      menu
    }
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

  override def createRouteMap(items: Seq[Item]): RouteCardCreated = {
    logger.info(s"Route Map creation...")

    val routeCard = items.map(item => RouteItem(item.id, item.routeStages))
    RouteCardCreated(routeCard)
  }
}
