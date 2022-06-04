package ru.misis.menu.model

import com.typesafe.config.ConfigFactory
import ru.misis.elastic.ConfigElasticClear

object MenuConfig {
  val clearElasticFieldName = "clear-elastic"

  def apply(): MenuConfig = {
    val config = ConfigFactory.load().getConfig("menu")
    val clearElasticValue = config.getBoolean(clearElasticFieldName)
    MenuConfig(
      clearElastic = clearElasticValue,
    )
  }
}

case class MenuConfig(
                       clearElastic: Boolean,
                     ) extends ConfigElasticClear
