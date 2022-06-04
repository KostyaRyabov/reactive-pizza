package ru.misis.orders.model

import com.typesafe.config.ConfigFactory
import ru.misis.elastic.ConfigElasticClear

object OrderConfig {
  val clearElasticFieldName = "clear-elastic"
  val waiterInstancesNumFieldName = "waiter-instances-num"

  def apply(): OrderConfig = {
    val config = ConfigFactory.load().getConfig("order")
    val clearElasticValue = config.getBoolean(clearElasticFieldName)
    val waiterInstancesNum = config.getInt(waiterInstancesNumFieldName)
    OrderConfig(
      clearElastic = clearElasticValue,
      waiterInstancesNum = waiterInstancesNum,
    )
  }
}

case class OrderConfig(
                        clearElastic: Boolean,
                        waiterInstancesNum: Int,
                      ) extends ConfigElasticClear
