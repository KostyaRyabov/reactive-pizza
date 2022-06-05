package ru.misis.payment.model

import com.typesafe.config.ConfigFactory
import ru.misis.elastic.ElasticClearSettings

object PaymentSettings {
  val clearElasticFieldName = "clear-elastic"

  def apply(): PaymentSettings = {
    val config = ConfigFactory.load().getConfig("payment")
    val clearElasticValue = config.getBoolean(clearElasticFieldName)
    PaymentSettings(
      clearElastic = clearElasticValue,
    )
  }
}

case class PaymentSettings(
                          clearElastic: Boolean,
                        ) extends ElasticClearSettings
