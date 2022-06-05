package ru.misis.payment.service

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import com.sksamuel.elastic4s.ElasticApi.properties
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.{ElasticClient, RequestSuccess}
import ru.misis.event.Cart._
import ru.misis.event.CartCreated
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

  override def confirm: CartCreated => Future[Done] = {
    case CartCreated(cart) =>
      val payment = Payment(
        id = cart.id,
        price = calculatePrice(cart),
      )

      logger.info(s"Payment#${payment.id}: ${payment.datetime} ...")

      for {
        _ <- elastic.execute {
          indexInto("payment").doc(payment)
        }
          .map({
            case results: RequestSuccess[IndexResponse] =>
              logger.info(s"Payment created: ${payment.toJson.prettyPrint}")
          })
        result <- Source.single(PaymentConfirmed(payment.id))
          .runWith(kafkaSink[PaymentConfirmed])
      } yield result
  }

  override def getList: Future[Seq[Payment]] = {
    elastic
      .execute(search("payment").matchAllQuery())
      .map({ case results: RequestSuccess[SearchResponse] => results.result.to[Payment] })
  }

  override def getPayment(id: String): Future[Payment] = {
    elastic.execute(get("payment", id))
      .map {
        case results: RequestSuccess[GetResponse] => results.result.to[Payment]
      }
  }
}
