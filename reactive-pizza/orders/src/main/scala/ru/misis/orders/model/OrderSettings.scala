package ru.misis.orders.model

import com.typesafe.config.ConfigFactory
import ru.misis.elastic.ElasticClearSettings

object OrderSettings {
  val clearElasticFieldName = "clear-elastic"
  val waiterInstancesNumFieldName = "waiter-instances-num"

  def apply(): OrderSettings = {
    val config = ConfigFactory.load().getConfig("order")
    val clearElasticValue = config.getBoolean(clearElasticFieldName)
    val waiterInstancesNum = config.getInt(waiterInstancesNumFieldName)
    OrderSettings(
      clearElastic = clearElasticValue,
      waiterInstancesNum = waiterInstancesNum,
    )
  }
}

case class OrderSettings(
                          clearElastic: Boolean,
                          waiterInstancesNum: Int,
                        ) extends ElasticClearSettings
