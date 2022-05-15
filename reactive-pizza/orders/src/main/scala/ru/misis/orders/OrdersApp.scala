package ru.misis.orders

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import ru.misis.orders.routes.OrderRoutes
import ru.misis.orders.service.{OrderCommandsImpl, OrderEventProcessing}
import ru.misis.payment.routes.PaymentRoutes
import ru.misis.payment.service.PaymentCommandsImpl
import ru.misis.util.HttpServer

import scala.concurrent.ExecutionContext.Implicits.global

object OrdersApp {
  val props = ElasticProperties("http://localhost:9200")
  val elastic = ElasticClient(JavaClient(props))

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("HelloAkkaHttpServer")

    val orderCommands = new OrderCommandsImpl(elastic)
    val orderRoutes = new OrderRoutes(orderCommands)
    val orderEventProcessing = new OrderEventProcessing(orderCommands)
    val server = new HttpServer("Orders", orderRoutes.routes, 8083)
    server.start()
  }
}
