package ru.misis.cart.model

import com.typesafe.config.ConfigFactory
import ru.misis.elastic.ElasticClearSettings

object CartSettings {
  val clearElasticFieldName = "clear-elastic"

  def apply(): CartSettings = {
    val config = ConfigFactory.load().getConfig("cart")
    val clearElasticValue = config.getBoolean(clearElasticFieldName)
    CartSettings(
      clearElastic = clearElasticValue,
    )
  }
}

case class CartSettings(
                       clearElastic: Boolean,
                     ) extends ElasticClearSettings
