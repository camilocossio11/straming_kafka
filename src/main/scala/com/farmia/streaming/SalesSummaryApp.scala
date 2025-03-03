package com.farmia.streaming

import org.apache.kafka.streams.kstream.{Aggregator, Consumed, Grouped, Initializer, KStream, KTable, Materialized, Produced, TimeWindows, Windowed}
import com.farmia.streaming.sales.models.{SalesSummary, SalesTransaction}
import org.apache.kafka.streams.{KafkaStreams, KeyValue, StreamsBuilder, StreamsConfig}
import org.apache.kafka.common.serialization.Serdes
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.state.{Stores, WindowStore}

import java.time.Duration
import java.util.Properties
import scala.jdk.CollectionConverters._

object SalesSummaryApp {
  def main(args: Array[String]): Unit = {
    val props = new Properties()
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, SalesSummaryConfig.appIdConfig)
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, SalesSummaryConfig.bootstrapServersConfig)
    props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass.getName)
    props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, classOf[SpecificAvroSerde[SalesTransaction]].getName)
    props.put("schema.registry.url", SalesSummaryConfig.schemaRegistryUrl)

    val builder = new StreamsBuilder()

    val avroSerdeSalesTransaction = new SpecificAvroSerde[SalesTransaction]
    avroSerdeSalesTransaction.configure(Map("schema.registry.url" -> SalesSummaryConfig.schemaRegistryUrl).asJava, false)

    val avroSerdeSalesSummary = new SpecificAvroSerde[SalesSummary]
    avroSerdeSalesSummary.configure(Map("schema.registry.url" -> SalesSummaryConfig.schemaRegistryUrl).asJava, false)

    val salesStream: KStream[String, SalesTransaction] = builder.stream(
      SalesSummaryConfig.inputTopicSummaryApp,
      Consumed.`with`(Serdes.String, avroSerdeSalesTransaction)
    )

    val categoryStream: KStream[String, SalesTransaction] = salesStream.selectKey(
      (_, transaction) => transaction.category
    )

    val groupedStream = categoryStream.groupByKey(Grouped.`with`(Serdes.String(), avroSerdeSalesTransaction))

    val salesSummaryInitializer: Initializer[SalesSummary] = () => new SalesSummary("", 0, 0.0, 0L, 0L)
    val salesSummaryAggregator: Aggregator[String, SalesTransaction, SalesSummary] =
      (category, sale, agg) => new SalesSummary(
        category,
        agg.getTotalQuantity + sale.getQuantity,
        agg.getTotalRevenue + sale.getPrice,
        agg.getWindowStart,
        agg.getWindowEnd
      )
    val materialized: Materialized[String, SalesSummary, WindowStore[Bytes, Array[Byte]]] =
      Materialized
        .as[String, SalesSummary](Stores.persistentWindowStore("sales-summary", Duration.ofMinutes(1), Duration.ofMinutes(1), false))
        .withKeySerde(Serdes.String())
        .withValueSerde(avroSerdeSalesSummary)
    val windowedStream: KTable[Windowed[String], SalesSummary] = groupedStream
      .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
      .aggregate(
        salesSummaryInitializer,
        salesSummaryAggregator,
        materialized
      )

    windowedStream.toStream.map {
      (windowedKey, summary) =>
        val categoryKey: String = windowedKey.key()
        val salesSummary = new SalesSummary(
          summary.getCategory,
          summary.getTotalQuantity,
          summary.getTotalRevenue,
          summary.getWindowStart,
          summary.getWindowEnd
        )
        KeyValue.pair(categoryKey, salesSummary)
    }.to(SalesSummaryConfig.outputTopicSummaryApp, Produced.`with`(Serdes.String(), avroSerdeSalesSummary))

    val streams = new KafkaStreams(builder.build(), props)
    streams.start()

    sys.addShutdownHook {
      streams.close()
    }
  }
}
