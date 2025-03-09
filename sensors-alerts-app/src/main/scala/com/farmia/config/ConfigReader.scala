package com.farmia.config

import com.typesafe.config.Config

// Type class for reading config values
trait ConfigReader[T] {
  def read(config: Config, path: String): T
}

object ConfigReader {
  implicit val stringReader: ConfigReader[String] = (config, path) =>
    if (config.hasPath(path)) config.getString(path).trim else throw new NoSuchElementException(s"Missing config: $path")

  implicit val intReader: ConfigReader[Int] = (config, path) =>
    if (config.hasPath(path)) config.getInt(path) else throw new NoSuchElementException(s"Missing config: $path")

  implicit val doubleReader: ConfigReader[Double] = (config, path) =>
    if (config.hasPath(path)) config.getDouble(path) else throw new NoSuchElementException(s"Missing config: $path")
}
