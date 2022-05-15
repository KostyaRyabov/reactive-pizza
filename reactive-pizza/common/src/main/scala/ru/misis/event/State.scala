package ru.misis.event

import ru.misis.event.State._

object State extends Enumeration {
  type State = String

  val Ready = "ready"
  val InProcess = "in process"
  val InWait = "in wait"
  val NotFound = "not found"
}


trait WithState {
  def state: State

  def isReady: Boolean = state == Ready

  def isInProcess: Boolean = state == InProcess

  def isInWait: Boolean = state == InWait

  def isNotFound: Boolean = state == NotFound
}
