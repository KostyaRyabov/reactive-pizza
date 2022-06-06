package ru.misis.orders.service

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import ru.misis.event.EventJsonFormats.orderJsonFormat
import ru.misis.event.Order
import ru.misis.orders.model.OrderCommands
import ru.misis.util.WithWait
import spray.json.enrichAny

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}


object Waiter extends WithWait {
  sealed trait Command
  case class TakeOrder(order: Order) extends Command
  case class Done(msg: String) extends Command
  case class Fail(msg: String) extends Command
  case class ReturnOrder(order: Order) extends Command

  override val delayRange: Range = 5 to 50

  def apply(service: OrderCommands)
           (implicit ec: ExecutionContext): Behavior[Command] = Behaviors.setup { context =>
    context.log.info("Starting waiter...")

    Behaviors.receiveMessage {
      case TakeOrder(order) =>
        waitAnyMillis()
        context.log.info(s"Take order: ${order.toJson.prettyPrint}")
        context.pipeToSelf(service.submit(order)) {
          case Success(_) => Done(order.id)
          case Failure(ex) => Fail(ex.toString)
        }
        Behaviors.same
      case ReturnOrder(order) =>
        waitAnyMillis()
        context.log.info(s"Return order${order.id}")
        context.pipeToSelf(service.returnOrder(order)) {
          case Success(_) => Done(order.id)
          case Failure(ex) => Fail(ex.toString)
        }
        Behaviors.same
      case Done(msg) =>
        context.log.info("Done: " + msg)
        Behaviors.same
      case Fail(msg) =>
        context.log.info(s"Fail: $msg")
        Behaviors.same
    }
  }
}

