package ru.misis.menu

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import ru.misis.menu.routes.{CartRoutes, MenuRoutes}
import ru.misis.menu.service.{CartCommandImpl, CartEventProcessing, MenuCommandImpl, MenuEventProcessing}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success

object MenuApp {
  private def startHttpServer(routes: Route, port: Int = 8080)(implicit system: ActorSystem): Unit = {
    import system.dispatcher

    val futureBinding = Http().newServerAt("localhost", port).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  val props = ElasticProperties("http://localhost:9200")
  val elastic = ElasticClient(JavaClient(props))

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("HelloAkkaHttpServer")

    val menuCommands = new MenuCommandImpl(elastic)
    val menuRoutes = new MenuRoutes(menuCommands)
    val menuEventProcessing = new MenuEventProcessing(menuCommands)
    startHttpServer(menuRoutes.routes)

    val cartCommands = new CartCommandImpl(elastic)
    val cartRoutes = new CartRoutes(cartCommands)
    val cartEventProcessing = new CartEventProcessing(cartCommands)
    startHttpServer(cartRoutes.routes, 8081)
  }
}
