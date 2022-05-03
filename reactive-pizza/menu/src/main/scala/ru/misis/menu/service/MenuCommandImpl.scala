package ru.misis.menu.service

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

import scala.concurrent.ExecutionContext

import scala.concurrent.{ExecutionContext, Future}

object MenuCommandImpl {
  val itemIndex = "item"
}

class MenuCommandImpl(elastic: ElasticClient)
                     (implicit executionContext: ExecutionContext,
                      val system: ActorSystem)
  extends MenuCommands
    with WithKafka {

  import MenuCommandImpl._

  elastic.execute {
    deleteIndex(itemIndex)
  }
    .flatMap { _ =>
      elastic.execute {
        createIndex(itemIndex).mapping(
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

  override def listItems(): Future[Seq[Item]] = {
    elastic.execute(search(itemIndex))
      .map {
        case results: RequestSuccess[SearchResponse] => results.result.to[Item]
      }

  }

  override def getItem(id: String): Future[Item] =
    elastic.execute(get(itemIndex, id))
      .map {
        case results: RequestSuccess[GetResponse] => results.result.to[Item]
      }

  override def findItem(name: String): Future[Seq[Item]] = {
    elastic.execute {
      search(itemIndex).query(
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
      indexInto(itemIndex).id(item.id).doc(item)
    }
      .map {
        case results: RequestSuccess[IndexResponse] => item
      }

  override def publish(itemIds: Seq[String]): Future[Done] = {
    for {
      items <- Future.sequence(itemIds.map { itemId =>
        elastic.execute(get(itemIndex, itemId))
          .map {
            case results: RequestSuccess[GetResponse] => results.result.to[Item]
          }
      })
      result <- Source.single(ItemsEvent(items))
        .runWith(kafkaSink[ItemsEvent])
    } yield result
  }
}
