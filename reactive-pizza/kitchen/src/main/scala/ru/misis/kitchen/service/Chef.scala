package ru.misis.kitchen.service

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import ru.misis.event.Menu.RouteItem
import ru.misis.event.{Menu, Order, OrderData, State}
import ru.misis.kitchen.model.KitchenCommands
import ru.misis.util.WithWait

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object Chef extends WithWait {
  sealed trait Command
  case class TakeItem(item: Order.ItemData) extends Command
  case class MakeStage(item: Order.ItemData, steps: Seq[Menu.RouteStage]) extends Command
  case class ReturnItem(item: Order.ItemData) extends Command

  override val delayRange: Range = 5 to 100

  def apply(service: KitchenCommands)(implicit ec: ExecutionContext): Behavior[Command] = Behaviors.setup { context =>
    context.log.info("Starting chef...")

    Behaviors.receiveMessage {
      case TakeItem(item) =>
        waitAnyMillis()
        for {
          RouteItem(_, routeStages) <- service.getRouteItem(item.id)
          _ <- service.submit(item.copy(state = State.InProcess))
        } yield {
          context.self ! MakeStage(item, routeStages)
        }
        Behaviors.same
      case MakeStage(item, routeStage :: nextRouteStages) =>
        waitAnyMillis()
        waitMillis(routeStage.duration)
        context.self ! MakeStage(item, nextRouteStages)
        Behaviors.same
      case MakeStage(item, _) =>
        waitAnyMillis()
        context.self ! ReturnItem(item)
        Behaviors.same
      case ReturnItem(item) =>
        waitAnyMillis()
        service.submit(item.copy(state = State.Ready))
        Behaviors.same
    }
  }
}
