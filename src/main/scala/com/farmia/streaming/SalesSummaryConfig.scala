package com.farmia.streaming

import com.typesafe.config.{Config, ConfigFactory}

object SalesSummaryConfig {
  private var config: Config = ConfigFactory.parseResources("application.conf").resolve()

  def getStringOrDefault(path: String, default: String): String = {
    if (config.hasPath(path) && config.getString(path).trim != null) config.getString(path).trim else default
  }

  def appIdConfig: String = getStringOrDefault("salesTransactions.appIdConfig", "sales-transactions-summary")
  def bootstrapServersConfig: String = getStringOrDefault(
    "salesTransactions.bootstrapServersConfig", "broker-1:29092,broker-2:29093,broker-3:29094"
  )
  def schemaRegistryUrl: String = getStringOrDefault(
    "salesTransactions.schemaRegistryUrl", "http://schema-registry:8081"
  )
  def inputTopicSummaryApp: String = getStringOrDefault("salesTransactions.inputTopic", "sales-transactions")
  def outputTopicSummaryApp: String = getStringOrDefault("salesTransactions.outputTopic", "sales-summary")
}
