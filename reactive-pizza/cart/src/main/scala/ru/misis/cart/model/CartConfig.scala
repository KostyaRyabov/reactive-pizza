package ru.misis.cart.model

import com.typesafe.config.ConfigFactory
import ru.misis.elastic.ConfigElasticClear

object CartConfig {
  val clearElasticFieldName = "clear-elastic"

  def apply(): CartConfig = {
    val config = ConfigFactory.load().getConfig("cart")
    val clearElasticValue = config.getBoolean(clearElasticFieldName)
    CartConfig(
      clearElastic = clearElasticValue,
    )
  }
}

case class CartConfig(
                       clearElastic: Boolean,
                     ) extends ConfigElasticClear
