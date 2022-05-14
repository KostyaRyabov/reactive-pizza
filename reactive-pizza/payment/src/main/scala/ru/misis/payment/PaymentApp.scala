package ru.misis.payment

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import ru.misis.payment.routes.PaymentRoutes
import ru.misis.payment.service.PaymentCommandsImpl
import ru.misis.util.HttpServer

import scala.concurrent.ExecutionContext.Implicits.global

object PaymentApp {
  val props = ElasticProperties("http://localhost:9200")
  val elastic = ElasticClient(JavaClient(props))

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("HelloAkkaHttpServer")

    val paymentCommands = new PaymentCommandsImpl(elastic)
    val paymentRoutes = new PaymentRoutes(paymentCommands)
    val server = new HttpServer("Payment", paymentRoutes.routes, 8082)
    server.start()
  }
}
