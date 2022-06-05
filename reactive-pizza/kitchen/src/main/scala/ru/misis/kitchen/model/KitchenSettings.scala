package ru.misis.kitchen.model

import com.typesafe.config.ConfigFactory
import ru.misis.elastic.ElasticClearSettings

object KitchenSettings {
  val clearElasticFieldName = "clear-elastic"
  val chefInstancesNumFieldName = "chef-instances-num"

  def apply(): KitchenSettings = {
    val config = ConfigFactory.load().getConfig("kitchen")
    val clearElasticValue = config.getBoolean(clearElasticFieldName)
    val chefInstancesNum = config.getInt(chefInstancesNumFieldName)
    KitchenSettings(
      clearElastic = clearElasticValue,
      chefInstancesNum = chefInstancesNum,
    )
  }
}

case class KitchenSettings(
                          clearElastic: Boolean,
                          chefInstancesNum: Int,
                        ) extends ElasticClearSettings
