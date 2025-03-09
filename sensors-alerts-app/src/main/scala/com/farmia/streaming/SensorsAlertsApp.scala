package com.farmia.streaming

import com.farmia.config.SensorsAlertsConfig
import com.farmia.iot.{SensorAlerts, SensorTelemetry}
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.kstream._
import org.apache.kafka.streams.{KafkaStreams, KeyValue, StreamsBuilder, StreamsConfig}

import java.util.Properties
import scala.jdk.CollectionConverters._

object SensorsAlertsApp {
  def main(args: Array[String]): Unit = {
    val props = defineStreamConfig()
    val tempThreshold: Double = SensorsAlertsConfig.temperatureThreshold
    val humThreshold: Double = SensorsAlertsConfig.humidityThreshold

    val builder = new StreamsBuilder()

    val avroSerdeSensorTelemetry: SpecificAvroSerde[SensorTelemetry] = createAvroSerde[SensorTelemetry]()
    val avroSerdeSensorAlerts: SpecificAvroSerde[SensorAlerts] = createAvroSerde[SensorAlerts]()

    val sensorStream: KStream[String, SensorTelemetry] = builder.stream(
      SensorsAlertsConfig.inputTopic,
      Consumed.`with`(Serdes.String, avroSerdeSensorTelemetry)
    )

    val alertTemperature = sensorStream
      .filter((_, sensor_info) => sensor_info.getTemperature > tempThreshold)
      .map((_, sensor_info) => createAlert(sensorInfo = sensor_info, temperature = true, threshold = tempThreshold))

    val alertHumidity = sensorStream
      .filter((_, sensor_info) => sensor_info.getHumidity > humThreshold)
      .map((_, sensor_info) => createAlert(sensorInfo = sensor_info, temperature = false, threshold = humThreshold))

    alertTemperature.to(
      SensorsAlertsConfig.outputTopic,
      Produced.`with`(Serdes.String(), avroSerdeSensorAlerts)
    )
    alertHumidity.to(
      SensorsAlertsConfig.outputTopic,
      Produced.`with`(Serdes.String(), avroSerdeSensorAlerts)
    )

    val streams = new KafkaStreams(builder.build(), props)
    streams.start()

    sys.addShutdownHook {
      streams.close()
    }
  }

  private def defineStreamConfig(): Properties = {
    val props = new Properties()
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, SensorsAlertsConfig.appIdConfig)
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, SensorsAlertsConfig.bootstrapServersConfig)
    props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass.getName)
    props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, classOf[SpecificAvroSerde[SensorTelemetry]].getName)
    props.put("schema.registry.url", SensorsAlertsConfig.schemaRegistryUrl)
    props
  }

  private def createAvroSerde[T <: SpecificRecord](): SpecificAvroSerde[T] ={
    val serde = new SpecificAvroSerde[T]()
    serde.configure(Map("schema.registry.url" -> SensorsAlertsConfig.schemaRegistryUrl).asJava, false)
    serde
  }

  private def createAlert(sensorInfo: SensorTelemetry, temperature: Boolean, threshold: Double):
  KeyValue[String, SensorAlerts] = {

    val (alertType, detail) =
      if (temperature) ("HIGH_TEMPERATURE", s"Temperature exceeded $threshold°C")
      else ("LOW_HUMIDITY", s"Humidity is below ${threshold * 100}%")

    KeyValue.pair(
      sensorInfo.getSensorId.toString,
      SensorAlerts.newBuilder()
        .setSensorId(sensorInfo.getSensorId)
        .setAlertType(alertType)
        .setTimestamp(sensorInfo.getTimestamp.toEpochMilli)
        .setDetails(detail)
        .build()
    )
  }

}
