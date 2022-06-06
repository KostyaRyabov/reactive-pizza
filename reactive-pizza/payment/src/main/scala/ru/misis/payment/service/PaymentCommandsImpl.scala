package ru.misis.payment.service

import akka.Done
import akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticApi.{dateField, doubleField, indexInto, keywordField, matchQuery, properties, search}
import com.sksamuel.elastic4s.ElasticDsl.{IndexHandler, SearchHandler}
import com.sksamuel.elastic4s.{ElasticClient, RequestSuccess}
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import ru.misis.event.Cart._
import ru.misis.event.EventJsonFormats.paymentConfirmedJsonFormat
import ru.misis.event.Payment.PaymentConfirmed
import ru.misis.payment.model.PaymentJsonFormats._
import ru.misis.payment.model.{Payment, PaymentCommands, PaymentSettings}
import ru.misis.util.{WithElasticInit, WithKafka, WithLogger}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

class PaymentCommandsImpl(val elastic: ElasticClient, val settings: PaymentSettings)
                         (implicit val executionContext: ExecutionContext, val system: ActorSystem)
  extends PaymentCommands
    with WithKafka
    with WithLogger
    with WithElasticInit {

  logger.info(s"Payment server initializing ...")

  initElastic("payment")(
    properties(
      keywordField("id"),
      doubleField("price"),
      dateField("datetime"),
    )
  )

  private def calculatePrice(cart: CartInfo): Double = cart.items.map(calculatePrice).sum

  private def calculatePrice(item: ItemInfo): Double = item.price * item.amount

  override def confirmCart(cart: CartInfo): Future[Done] = {
    val payment = Payment(
      id = cart.id,
      price = calculatePrice(cart),
    )

    logger.info(s"Payment#${payment.id}: ${payment.datetime} ...")

    for {
      _ <- elastic.execute(indexInto("payment").doc(payment))
        .map({
          case results: RequestSuccess[IndexResponse] =>
            logger.info(s"Payment created: ${payment.toJson.prettyPrint}")
        })
      result <- publishEvent(PaymentConfirmed(payment.id))
    } yield result
  }

  override def getList: Future[Seq[Payment]] = {
    elastic
      .execute(search("payment").matchAllQuery())
      .map({ case results: RequestSuccess[SearchResponse] => results.result.to[Payment] })
  }

  override def getPayment(id: String): Future[Payment] = {
    elastic.execute(search("payment").query(matchQuery("id", id)))
      .map({ case results: RequestSuccess[SearchResponse] => results.result.to[Payment].head })
  }
}
