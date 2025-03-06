package com.farmia.streaming

import com.farmia.sales.{sales_summary, sales_transactions}
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.kstream._
import org.apache.kafka.streams.state.{Stores, WindowStore}
import org.apache.kafka.streams.{KafkaStreams, KeyValue, StreamsBuilder, StreamsConfig}

import java.time.Duration
import java.util.Properties
import scala.jdk.CollectionConverters._

object SalesSummaryApp {
  def main(args: Array[String]): Unit = {
    val props = new Properties()
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, SalesSummaryConfig.appIdConfig)
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, SalesSummaryConfig.bootstrapServersConfig)
    props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass.getName)
    props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, classOf[SpecificAvroSerde[sales_transactions]].getName)
    props.put("schema.registry.url", SalesSummaryConfig.schemaRegistryUrl)

    val builder = new StreamsBuilder()

    val avroSerdeSalesTransaction = new SpecificAvroSerde[sales_transactions]
    avroSerdeSalesTransaction.configure(Map("schema.registry.url" -> SalesSummaryConfig.schemaRegistryUrl).asJava, false)

    val avroSerdeSalesSummary = new SpecificAvroSerde[sales_summary]
    avroSerdeSalesSummary.configure(Map("schema.registry.url" -> SalesSummaryConfig.schemaRegistryUrl).asJava, false)

    val salesStream: KStream[String, sales_transactions] = builder.stream(
      SalesSummaryConfig.inputTopicSummaryApp,
      Consumed.`with`(Serdes.String, avroSerdeSalesTransaction)
    )

    val categoryStream: KStream[String, sales_transactions] = salesStream.selectKey(
      (_, transaction) => transaction.get(2).toString
    )

    val groupedStream = categoryStream.groupByKey(Grouped.`with`(Serdes.String(), avroSerdeSalesTransaction))

    val salesSummaryInitializer: Initializer[sales_summary] = () => new sales_summary("", 0, 0.0, 0L, 0L)
    val salesSummaryAggregator: Aggregator[String, sales_transactions, sales_summary] =
      (category, sale, agg) => new sales_summary(
        category,
        agg.getTotalQuantity + sale.getQuantity,
        agg.getTotalRevenue + sale.getPrice,
        agg.getWindowStart,
        agg.getWindowEnd
      )
    val materialized: Materialized[String, sales_summary, WindowStore[Bytes, Array[Byte]]] =
      Materialized
        .as[String, sales_summary](Stores.persistentWindowStore("sales-summary", Duration.ofMinutes(1), Duration.ofMinutes(1), false))
        .withKeySerde(Serdes.String())
        .withValueSerde(avroSerdeSalesSummary)

    val windowedStream: KTable[Windowed[String], sales_summary] = groupedStream
      .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
      .aggregate(
        salesSummaryInitializer,
        salesSummaryAggregator,
        materialized
      )

    windowedStream.toStream.map {
      (windowedKey, summary) =>
        val categoryKey: String = windowedKey.key()
        val salesSummary = new sales_summary(
          summary.getCategory,
          summary.getTotalQuantity,
          summary.getTotalRevenue,
          windowedKey.window().start(),
          windowedKey.window().end()
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
