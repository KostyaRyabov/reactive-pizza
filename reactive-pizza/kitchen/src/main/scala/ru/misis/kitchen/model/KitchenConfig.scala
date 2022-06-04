package ru.misis.kitchen.model

import com.typesafe.config.ConfigFactory
import ru.misis.elastic.ConfigElasticClear

object KitchenConfig {
  val clearElasticFieldName = "clear-elastic"
  val chefInstancesNumFieldName = "chef-instances-num"

  def apply(): KitchenConfig = {
    val config = ConfigFactory.load().getConfig("kitchen")
    val clearElasticValue = config.getBoolean(clearElasticFieldName)
    val chefInstancesNum = config.getInt(chefInstancesNumFieldName)
    KitchenConfig(
      clearElastic = clearElasticValue,
      chefInstancesNum = chefInstancesNum,
    )
  }
}

case class KitchenConfig(
                          clearElastic: Boolean,
                          chefInstancesNum: Int,
                        ) extends ConfigElasticClear
