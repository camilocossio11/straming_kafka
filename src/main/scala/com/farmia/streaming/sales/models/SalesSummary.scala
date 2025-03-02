package com.farmia.streaming.sales.models

import com.farmia.streaming.SalesSummaryConfig

import org.apache.avro.Schema
import org.apache.avro.specific.SpecificRecordBase

case class SalesSummary(
   var category: String,
   var total_quantity: Int,
   var total_revenue: Double,
   var window_start: Long,
   var window_end: Long
 ) extends SpecificRecordBase {

  def this() = this("", 0, 0.0, 0L, 0L)

  override def getSchema: Schema = SalesSummary.SCHEMA$

  override def get(field: Int): AnyRef = field match {
    case 0 => category
    case 1 => Integer.valueOf(total_quantity)
    case 2 => java.lang.Double.valueOf(total_revenue)
    case 3 => java.lang.Long.valueOf(window_start)
    case 4 => java.lang.Long.valueOf(window_end)
  }

  override def put(field: Int, value: Any): Unit = field match {
    case 0 => category = value.toString
    case 1 => total_quantity = value.asInstanceOf[Int]
    case 2 => total_revenue = value.asInstanceOf[Double]
    case 3 => window_start = value.asInstanceOf[Long]
    case 4 => window_end = value.asInstanceOf[Long]
  }

  def getCategory: String = category
  def getTotalQuantity: Int = total_quantity
  def getTotalRevenue: Double = total_revenue
  def getWindowStart: Long = window_start
  def getWindowEnd: Long = window_end

}

private object SalesSummary {
  private val SCHEMA$: Schema = new Schema.Parser().parse(SalesSummaryConfig.summarySchema)
}

