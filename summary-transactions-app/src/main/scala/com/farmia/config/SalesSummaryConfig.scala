package com.farmia.config

import com.typesafe.config.{Config, ConfigFactory}

object SalesSummaryConfig {
  private var config: Config = ConfigFactory.parseResources("application.conf").resolve()

  def getOrDefault[T](path: String, default: T)(implicit reader: ConfigReader[T]): T = {
    try {
      reader.read(config, path)
    } catch {
      case _: Exception => default
    }
  }

  def appIdConfig: String = getOrDefault("salesTransactions.appIdConfig", "sales-transactions-summary")
  def bootstrapServersConfig: String = getOrDefault(
    "salesTransactions.bootstrapServersConfig", "broker-1:29092,broker-2:29093,broker-3:29094"
  )
  def schemaRegistryUrl: String = getOrDefault(
    "salesTransactions.schemaRegistryUrl", "http://schema-registry:8081"
  )
  def inputTopicSummaryApp: String = getOrDefault("salesTransactions.inputTopic", "sales-transactions")
  def outputTopicSummaryApp: String = getOrDefault("salesTransactions.outputTopic", "sales-summary")
  def windowSizeMinutes: Int = getOrDefault("salesTransactions.windowSizeMinutes", 1)
}
