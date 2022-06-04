package ru.misis.kitchen

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import ru.misis.kitchen.model.KitchenConfig
import ru.misis.kitchen.routes.KitchenRoutes
import ru.misis.kitchen.service.{KitchenCommandsImpl, KitchenEventProcessing}
import ru.misis.util.HttpServer

import scala.concurrent.ExecutionContext.Implicits.global

object PaymentApp {
  val props = ElasticProperties("http://localhost:9200")
  val elastic = ElasticClient(JavaClient(props))

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("HelloAkkaHttpServer")

    val config = KitchenConfig()
    val kitchenCommands = new KitchenCommandsImpl(elastic, config)
    val kitchenRoutes = new KitchenRoutes(kitchenCommands)
    val kitchenEventProcessing = new KitchenEventProcessing(kitchenCommands)
    val server = new HttpServer("Payment", kitchenRoutes.routes, 8084)
    server.start()
  }
}
