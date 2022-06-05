package ru.misis.menu.model

import com.typesafe.config.ConfigFactory
import ru.misis.elastic.ElasticClearSettings

object MenuSettings {
  val clearElasticFieldName = "clear-elastic"

  def apply(): MenuSettings = {
    val config = ConfigFactory.load().getConfig("menu")
    val clearElasticValue = config.getBoolean(clearElasticFieldName)
    MenuSettings(
      clearElastic = clearElasticValue,
    )
  }
}

case class MenuSettings(
                       clearElastic: Boolean,
                     ) extends ElasticClearSettings
