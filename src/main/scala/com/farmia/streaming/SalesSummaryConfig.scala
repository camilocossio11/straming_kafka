package com.farmia.streaming

import com.typesafe.config.{Config, ConfigFactory}

object SalesSummaryConfig {
  private var config: Config = ConfigFactory.parseResources("application.conf").resolve()

  def getStringOrDefault(path: String, default: String): String = {
    if (config.hasPath(path) && config.getString(path).trim != null) config.getString(path).trim else default
  }

  def transactionsSchema: String = config.getString("salesTransactions.schema")
  def summarySchema: String = config.getString("salesTransactions.schemaSummary")
  def appIdConfig: String = getStringOrDefault("salesTransactions.appIdConfig", "sales-transactions-summary")
  def bootstrapServersConfig: String = getStringOrDefault(
    "salesTransactions.bootstrapServersConfig", "broker1:9092,broker2:9093,broker3:9094"
  )
  def schemaRegistryUrl: String = getStringOrDefault(
    "salesTransactions.schemaRegistryUrl", "http://schema-registry:8081"
  )
  def inputTopicSummaryApp: String = getStringOrDefault("salesTransactions.inputTopic", "sales-transactions")
  def outputTopicSummaryApp: String = getStringOrDefault("salesTransactions.outputTopic", "sales-summary")
}
