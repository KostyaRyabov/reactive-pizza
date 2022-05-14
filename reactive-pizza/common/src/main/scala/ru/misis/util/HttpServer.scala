package ru.misis.util

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import scala.util.{Failure, Success}

class HttpServer(name: String, routes: Route, port: Int)(implicit system: ActorSystem) {
  def start(): Unit = {
    import system.dispatcher

    val futureBinding = Http().newServerAt("localhost", port).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("{}: Server online at http://{}:{}/", name, address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("{}: Failed to bind HTTP endpoint, terminating system", name, ex)
        system.terminate()
    }
  }
}
