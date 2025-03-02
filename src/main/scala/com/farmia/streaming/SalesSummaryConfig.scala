package com.farmia.streaming

import com.typesafe.config.{ConfigFactory, Config}
import scala.jdk.CollectionConverters._

object SalesSummaryConfig {
  private var config: Config = ConfigFactory.load()

  def loadTestingConfig(): Unit = {
    config = ConfigFactory.load("application-test.conf")
  }

  def transactionsSchema: String = config.getString("salesTransactions.schema")
  def summarySchema: String = config.getString("salesTransactions.schemaSummary")
  def appIdConfig: String = config.getString("salesTransactions.appIdConfig")
  def bootstrapServersConfig: String = config.getString("salesTransactions.bootstrapServersConfig")
  def schemaRegistryUrl: String = config.getString("salesTransactions.schemaRegistryUrl")
}
