package ru.misis.menu.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ru.misis.event.Cart.Item
import ru.misis.event.EventJsonFormats._
import ru.misis.menu.model.CartCommands

class CartRoutes(cartService: CartCommands)(implicit val system: ActorSystem) {

  val routes: Route = {
    path("menu") {
      get {
        onSuccess(cartService.getMenu) { menu =>
          complete((StatusCodes.OK, menu))
        }
      }
    } ~
    path("carts") {
      (post & entity(as[Item])) { item =>
        onSuccess(cartService.addItem(item)) { itemInfo =>
          complete((StatusCodes.Created, itemInfo))
        }
      } ~
      (put & entity(as[Item])) { item =>
        onSuccess(cartService.putItem(item)) {
          case Left(_) =>
            complete(StatusCodes.OK)
          case Right(item) =>
            complete((StatusCodes.OK, item))
        }
      } ~
        path(Segment) { cartId =>
          get {
            onSuccess(cartService.getCart(cartId)) { cartInfo =>
              complete((StatusCodes.OK, cartInfo))
            }
          } ~
            path("item" / Segment) { itemId =>
              delete {
                onSuccess(cartService.deleteItem(itemId, cartId)) { _ =>
                  complete(StatusCodes.OK)
                }
              }
            } ~
            delete {
              onSuccess(cartService.deleteCart(cartId)) { _ =>
                complete(StatusCodes.OK)
              }
            } ~
            path("pay") {
              get {
                onSuccess(cartService.pay(cartId)) { _ =>
                  complete(StatusCodes.OK)
                }
              }
            }
        }
    }
  }
}
