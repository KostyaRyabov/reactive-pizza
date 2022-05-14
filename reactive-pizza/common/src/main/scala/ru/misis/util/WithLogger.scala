package ru.misis.util

import org.slf4j.LoggerFactory

trait WithLogger {
  val logger = LoggerFactory.getLogger(this.getClass)
}
