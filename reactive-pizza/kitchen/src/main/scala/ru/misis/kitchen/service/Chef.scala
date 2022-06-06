package ru.misis.kitchen.service

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import ru.misis.event.{Menu, Order, State}
import ru.misis.kitchen.model.KitchenCommands
import ru.misis.util.WithWait

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Chef extends WithWait {
  sealed trait Command

  case class TakeItem(item: Order.ItemData) extends Command

  case class MakeStage(item: Order.ItemData, steps: Seq[Menu.RouteStage]) extends Command

  case class Done(msg: String) extends Command

  case class Fail(msg: String) extends Command

  case class ReturnItem(item: Order.ItemData) extends Command

  override val delayRange: Range = 5 to 100

  def apply(service: KitchenCommands)(implicit ec: ExecutionContext): Behavior[Command] = Behaviors.setup { context =>
    context.log.info("Starting chef...")

    Behaviors.receiveMessage {
      case TakeItem(item) =>
        waitAnyMillis()
        context.log.info(s"Take item#${item.id}")
        val routeStagesFuture = for {
          routeItem <- service.getRouteItem(item.menuItemId)
          _ <- service.submit(item.copy(state = State.InProcess))
        } yield {
          routeItem.routeStages
        }
        context.pipeToSelf(routeStagesFuture) {
          case Success(stages) => MakeStage(item, stages)
          case Failure(ex) => Fail(ex.toString)
        }
        Behaviors.same
      case MakeStage(item, routeStage :: nextRouteStages) =>
        waitAnyMillis()
        context.log.info(s"MakeStage item#${item.id}:${item.menuItemId} [left ${nextRouteStages.length} stages] - ${routeStage.name}")
        waitMillis(routeStage.duration)
        context.self ! MakeStage(item, nextRouteStages)
        Behaviors.same
      case MakeStage(item, _) =>
        context.log.info(s"MakeStage item#${item.id}:${item.menuItemId} [end]")
        waitAnyMillis()
        context.self ! ReturnItem(item)
        Behaviors.same
      case ReturnItem(item) =>
        waitAnyMillis()
        context.log.info(s"Return item#${item.id}:${item.menuItemId}")
        context.pipeToSelf(service.submit(item.copy(state = State.Ready))) {
          case Success(_) => Done(item.id)
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
