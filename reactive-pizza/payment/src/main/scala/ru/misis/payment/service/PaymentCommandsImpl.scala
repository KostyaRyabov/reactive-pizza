package ru.misis.payment.service

import akka.Done
import akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticApi.{createIndex, deleteIndex, properties}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.{ElasticClient, RequestSuccess}
import ru.misis.event.Cart._
import ru.misis.event.EventJsonFormats.paymentConfirmedFormat
import ru.misis.event.OrderData.OrderFormed
import ru.misis.event.Payment.PaymentConfirmed
import ru.misis.payment.model.PaymentJsonFormats._
import ru.misis.payment.model.{Bill, PaymentCommands}
import ru.misis.util.{WithKafka, WithLogger}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

class PaymentCommandsImpl(
                           elastic: ElasticClient,
                         )(implicit
                           executionContext: ExecutionContext,
                           val system: ActorSystem,
                         )
  extends PaymentCommands
    with WithKafka
    with WithLogger {

  logger.info(s"Payment server initializing ...")

  elastic.execute(
    deleteIndex("payment")
  )
    .flatMap(_ =>
      elastic.execute(
        createIndex("payment").mapping(
          properties(
            keywordField("id"),
            doubleField("price"),
          )
        )
      )
    )

  private def calculatePrice(cart: CartInfo): Double = cart.items.map(calculatePrice).sum

  private def calculatePrice(item: ItemInfo): Double = item.price * item.amount

  override def create: OrderFormed => Future[Bill] = {
    case OrderFormed(cart) =>
      logger.info(s"Preparing bill for payment...")

      val bill = Bill(
        id = cart.id,
        price = calculatePrice(cart),
      )

      elastic.execute {
        indexInto("payment").doc(bill)
      }
        .map({
          case results: RequestSuccess[IndexResponse] =>
            logger.info(s"Bill created: ${bill.toJson.prettyPrint}")
            bill
        })
  }

  override def confirm(id: String): Future[Done] = {
    logger.info("Payment confirmation of bill#{} in process.", id)

    for {
      _ <- elastic.execute(deleteById("payment", id))
      result <- publishEvent(PaymentConfirmed(id))
    } yield result
  }

  override def getUnpaidOrders: Future[Seq[Bill]] = {
    elastic
      .execute(search("payment").matchAllQuery())
      .map({ case results: RequestSuccess[SearchResponse] => results.result.to[Bill] })
  }
}
