package ru.misis.menu

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import ru.misis.menu.model.MenuSettings
import ru.misis.menu.routes.MenuRoutes
import ru.misis.menu.service.{MenuCommandsImpl, MenuEventProcessing}
import ru.misis.util.HttpServer

import scala.concurrent.ExecutionContext.Implicits.global

object MenuApp {
  val props = ElasticProperties("http://localhost:9200")
  val elastic = ElasticClient(JavaClient(props))

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("MenuHttpServer")

    val config = MenuSettings()
    val menuCommands = new MenuCommandsImpl(elastic, config)
    val menuRoutes = new MenuRoutes(menuCommands)
    val menuEventProcessing = new MenuEventProcessing(menuCommands)
    val server = new HttpServer("Menu", menuRoutes.routes, 8080)
    server.start()
  }
}
