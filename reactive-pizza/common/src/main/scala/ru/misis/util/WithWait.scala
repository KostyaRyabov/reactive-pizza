package ru.misis.util

import java.util.concurrent.ThreadLocalRandom

trait WithWait {
  val delayRange: Range

  private def randomDuration: Long = ThreadLocalRandom.current().nextLong(delayRange.start, delayRange.end + 1)

  def waitAnyMillis(): Unit = Thread.sleep(randomDuration)

  def waitMillis(duration: Long): Unit = Thread.sleep(duration)
}
