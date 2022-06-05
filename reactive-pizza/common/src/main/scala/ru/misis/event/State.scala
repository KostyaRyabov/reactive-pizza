package ru.misis.event

object State extends Enumeration {
  type State = String

  val Completed = "completed"
  val Ready = "ready"
  val InProcess = "in process"
  val InWait = "in wait"
  val NotFound = "not found"
}


trait WithState {
  import ru.misis.event.State._

  def state: State

  def isCompleted: Boolean = state == Completed

  def isReady: Boolean = state == Ready

  def isInProcess: Boolean = state == InProcess

  def isInWait: Boolean = state == InWait

  def isNotFound: Boolean = state == NotFound
}

trait WithItemsState {
  import ru.misis.event.State._

  def items: Seq[WithState]

  def getItemsSumState: State = {
    if (items.forall(_.isCompleted)) State.Completed
    else if (items.forall(_.isReady)) State.Ready
    else if (items.exists(_.isInProcess)) State.InProcess
    else if (items.forall(_.isInWait)) State.InWait
    else State.NotFound
  }

  def isCompleted: Boolean = getItemsSumState == Completed

  def isReady: Boolean = getItemsSumState == Ready

  def isInProcess: Boolean = getItemsSumState == InProcess

  def isInWait: Boolean = getItemsSumState == InWait

  def isNotFound: Boolean = getItemsSumState == NotFound
}
