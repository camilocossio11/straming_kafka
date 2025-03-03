resolvers += "Confluent" at "https://packages.confluent.io/maven/"

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .enablePlugins(AssemblyPlugin)
  .settings(
    name := "streaming_kafka",

    Compile / assembly / mainClass := Some("com.farmia.streaming.SalesSummaryApp"),

    assemblyMergeStrategy := {
      case PathList("META-INF", _ @_*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    },

    libraryDependencies ++= Seq(
      "org.apache.kafka" %% "kafka-streams-scala" % "3.5.1",
      "io.confluent" % "kafka-streams-avro-serde" % "7.5.0",
      "io.confluent" % "kafka-avro-serializer" % "7.5.0",
      "io.confluent" % "kafka-schema-registry-client" % "7.5.0",
      "com.typesafe" % "config" % "1.4.2",
      "org.apache.avro" % "avro" % "1.11.0",
      "org.apache.avro" % "avro-compiler" % "1.11.0",
      "org.slf4j" % "slf4j-simple" % "1.7.36"
    )
  )