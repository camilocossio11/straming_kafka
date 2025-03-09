package com.farmia.config

import com.typesafe.config.{Config, ConfigFactory}

object SensorsAlertsConfig {
  private var config: Config = ConfigFactory.parseResources("application.conf").resolve()

  def getOrDefault[T](path: String, default: T)(implicit reader: ConfigReader[T]): T = {
    try {
      reader.read(config, path)
    } catch {
      case _: Exception => default
    }
  }

  def appIdConfig: String = getOrDefault("salesTransactions.appIdConfig", "sensors-alerts-app")
  def bootstrapServersConfig: String = getOrDefault(
    "salesTransactions.bootstrapServersConfig", "broker-1:29092,broker-2:29093,broker-3:29094"
  )
  def schemaRegistryUrl: String = getOrDefault(
    "salesTransactions.schemaRegistryUrl", "http://schema-registry:8081"
  )
  def inputTopic: String = getOrDefault("salesTransactions.inputTopic", "sensor-telemetry")
  def outputTopic: String = getOrDefault("salesTransactions.outputTopic", "sensor-alerts")
  def temperatureThreshold: Double = getOrDefault("salesTransactions.temperatureThreshold", 35.0)
  def humidityThreshold: Double = getOrDefault("salesTransactions.humidityThreshold", 0.2)
}
