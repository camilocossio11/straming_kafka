package com.farmia.streaming.sales.models

import com.farmia.streaming.SalesSummaryConfig

import org.apache.avro.Schema
import org.apache.avro.specific.SpecificRecordBase

case class SalesTransaction(
  var transaction_id: String,
  var product_id: String,
  var category: String,
  var quantity: Int,
  var price: Double,
  var timestamp: Long
) extends SpecificRecordBase {

  def this() = this("", "", "", 0, 0.0, 0L)

  override def getSchema: Schema = SalesTransaction.SCHEMA$

  override def get(field: Int): AnyRef = field match {
    case 0 => transaction_id
    case 1 => product_id
    case 2 => category
    case 3 => Integer.valueOf(quantity)
    case 4 => java.lang.Double.valueOf(price)
    case 5 => java.lang.Long.valueOf(timestamp)
  }

  override def put(field: Int, value: Any): Unit = field match {
    case 0 => transaction_id = value.toString
    case 1 => product_id = value.toString
    case 2 => category = value.toString
    case 3 => quantity = value.asInstanceOf[Int]
    case 4 => price = value.asInstanceOf[Double]
    case 5 => timestamp = value.asInstanceOf[Long]
  }

  def getTransactionId: String = transaction_id
  def getProductId: String = product_id
  def getCategory: String = category
  def getQuantity: Int = quantity
  def getPrice: Double = price
}

private object SalesTransaction {
  private val SCHEMA$: Schema = new Schema.Parser().parse(SalesSummaryConfig.transactionsSchema)
}
