package ru.misis.payment.model

import com.typesafe.config.ConfigFactory
import ru.misis.elastic.ConfigElasticClear

object PaymentConfig {
  val clearElasticFieldName = "clear-elastic"

  def apply(): PaymentConfig = {
    val config = ConfigFactory.load().getConfig("payment")
    val clearElasticValue = config.getBoolean(clearElasticFieldName)
    PaymentConfig(
      clearElastic = clearElasticValue,
    )
  }
}

case class PaymentConfig(
                          clearElastic: Boolean,
                        ) extends ConfigElasticClear
