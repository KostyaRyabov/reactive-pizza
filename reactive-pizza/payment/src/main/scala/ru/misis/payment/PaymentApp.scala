package ru.misis.payment

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import ru.misis.payment.model.PaymentSettings
import ru.misis.payment.routes.PaymentRoutes
import ru.misis.payment.service.{PaymentCommandsImpl, PaymentEventProcessing}
import ru.misis.util.HttpServer

import scala.concurrent.ExecutionContext.Implicits.global

object PaymentApp {
  val props = ElasticProperties("http://localhost:9200")
  val elastic = ElasticClient(JavaClient(props))

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("PaymentHttpServer")

    val config = PaymentSettings()
    val paymentCommands = new PaymentCommandsImpl(elastic, config)
    val paymentRoutes = new PaymentRoutes(paymentCommands)
    val paymentEventProcessing = new PaymentEventProcessing(paymentCommands)
    val server = new HttpServer("Payment", paymentRoutes.routes, 8082)
    server.start()
  }
}
