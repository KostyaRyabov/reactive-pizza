package ru.misis.cart.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.scalaland.chimney.dsl.TransformerOps
import ru.misis.cart.model.CartCommands
import ru.misis.event.Cart._
import ru.misis.event.EventJsonFormats._

class CartRoutes(cartService: CartCommands) {

  val routes: Route = {
    path("carts") {
      (post & entity(as[Item])) { item =>
        onSuccess(cartService.addItem(item)) { itemInfo =>
          complete((StatusCodes.Created, itemInfo))
        }
      } ~
        (post & entity(as[ItemDTO])) { itemDTO =>
          val item = itemDTO.into[Item].transform
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
        }
    } ~
      pathPrefix("cart" / Segment) { cartId =>
        path("item" / Segment) { itemId =>
          delete {
            onSuccess(cartService.deleteItem(itemId, cartId)) { _ =>
              complete(StatusCodes.OK)
            }
          }
        } ~
          get {
            onSuccess(cartService.getCart(cartId)) { cartInfo =>
              complete((StatusCodes.OK, cartInfo))
            }
          } ~
          delete {
            onSuccess(cartService.deleteCart(cartId)) { _ =>
              complete(StatusCodes.OK)
            }
          }
      } ~
      path("pay" / Segment) { cartId =>
        get {
          onSuccess(cartService.pay(cartId)) { _ =>
            complete(StatusCodes.OK)
          }
        }
      }
  }
}
