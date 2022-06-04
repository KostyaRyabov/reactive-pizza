package ru.misis.orders.service

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import ru.misis.event.Order
import ru.misis.orders.model.OrderCommands
import ru.misis.util.WithWait

import scala.concurrent.ExecutionContext


object Waiter extends WithWait {
  sealed trait Command

  case class TakeOrder(order: Order) extends Command

  case class ReturnOrder(order: Order) extends Command

  override val delayRange: Range = 5 to 50

  def apply(service: OrderCommands)
           (implicit ec: ExecutionContext): Behavior[Command] = Behaviors.setup { context =>
    context.log.info("Starting waiter...")

    Behaviors.receiveMessage {
      case TakeOrder(order) =>
        waitAnyMillis()
        service.submit(order)
        Behaviors.same
      case ReturnOrder(order) =>
        waitAnyMillis()
        service.returnOrder(order)
        Behaviors.same
    }
  }
}

