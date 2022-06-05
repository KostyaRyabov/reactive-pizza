package ru.misis.cart

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import ru.misis.cart.model.CartSettings
import ru.misis.cart.routes.CartRoutes
import ru.misis.cart.service.{CartCommandsImpl, CartEventProcessing}
import ru.misis.util.HttpServer

import scala.concurrent.ExecutionContext.Implicits.global

object CartApp {
  val props = ElasticProperties("http://localhost:9200")
  val elastic = ElasticClient(JavaClient(props))

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("CartHttpServer")

    val config = CartSettings()
    val cartCommands = new CartCommandsImpl(elastic, config)
    val cartRoutes = new CartRoutes(cartCommands)
    val cartEventProcessing = new CartEventProcessing(cartCommands)
    val server = new HttpServer("Cart", cartRoutes.routes, 8081)
    server.start()
  }
}
