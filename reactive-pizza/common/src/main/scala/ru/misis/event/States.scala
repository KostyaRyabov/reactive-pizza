package ru.misis.event

abstract class State(val value: Short)


object States {
  case object Ready extends State(2) {
    override def toString: String = "ready"
  }

  case object InProcess extends State(1) {
    override def toString: String = "in process"
  }

  case object InWait extends State(0) {
    override def toString: String = "in wait"
  }

  case object NotFound extends State(-1) {
    override def toString: String = "not found"
  }

}

object StateImpl {
  implicit def toShort(state: State): Short = state.value
}
