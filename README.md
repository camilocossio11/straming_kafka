# Kafka Streams Project: Farmia Case Study

## Overview
The goal of this project is to build a Kafka cluster capable of simulating a real-time data stream. The cluster consists
of:

- **3 brokers** and **3 controllers**
- **Schema registry**
- **4 connectors**
- **2 Kafka Streams applications**
- **1 relational database**

## Background
This project simulates a real-world streaming use case for a fictitious company called **Farmia**, which specializes in
selling agricultural products.

Farmia operates a relational database that records sales transactions. Additionally, the company uses an IoT system to 
monitor production processes through a network of sensors that continuously send telemetry data.

## Applications
This project includes two Kafka Streams applications designed to process real-time data from sales and sensor telemetry.

### 1. `sales-transactions-summary-app`
This application summarizes sales transactions by aggregating data by product within **1-minute windows**.

**Output fields:**
- **Category** – Product category
- **Total quantity sold** – Total number of units sold
- **Total revenue received** – Total sales revenue
- **Window start timestamp** – Start time of the aggregation window
- **Window end timestamp** – End time of the aggregation window

**Example output:**
```json
{
  "category": "pesticides",
  "total_quantity": 31,
  "total_revenue": 272.54,
  "window_start": 1741505220000,
  "window_end": 1741505280000
}
```

**How it works:**
- The application reads data from the `sales-transactions` topic, populated by a source connector that pulls data from 
the relational database.
- The aggregated results are written to the `sales-summary` topic.

### 2. `sensors-alerts-app`

This application generates alerts when specific conditions are met:
- Temperature exceeds 35°C
- Humidity falls below 20%

**Example output:**
```json
{
  "sensor_id": "sensor_991",
  "alert_type": "LOW_HUMIDITY",
  "timestamp": 1741421301,
  "details": "Humidity is below 20.0%"
}
```

**How it works:**
- The application reads data from the `sensor-telemetry` topic, which receives real-time data from production system 
sensors.
- Alerts are written to the `sensor-alerts` topic.

### Configuration

Both applications were developed in **Scala**. The configuration settings, such as the aggregation window and alert 
thresholds, can be adjusted through environment variables in the **Docker Compose** file.

**Modifiable Settings:**
- **Aggregation window** – Defined in **minutes** and must be greater than 0.
- **Temperature threshold** – Must be greater than 0.
- **Humidity threshold** – Must be between 0 and 1 (representing a percentage).

**How to Modify Settings:**

To adjust these values:
1.	Open the Docker Compose file.
2.	Edit the relevant environment variables.
3.	Rebuild the compose using `docker compose build` command.

## Architecture

Below is an illustration of the project workflow. The resources in the `Data Generation` group are used to generate 
simulated data, while the resources in the `Streaming Process` group handle real-time data processing.

![til](./assets/kafka.drawio.png)

To simulate a real-world scenario and create interactivity in the transactions table located in the database, a couple
of connectors are used to continuously load data into it. For this, **_transactions** topic is used.

- **source-datagen-_transactions:** Generates transactions that comply the defined schema.
- **sink-mysql-_transactions:** Writes the transactions in a table of the database.
- **source-mysql-transactions:** Writes the transactions from the operational database to a Kafka topic.
- **source-datagen-sensor-telemetry:**: Generates telemetry that comply the defined schema.

## Organization

The project is organized in the following directories:

- **datagen**: Contains the schemas of the two datasets of Farmia sources (*.avsc)
- **connectors**: Contains the configuration files of the connectors (*.json)
- **docker**: Contains the Docker Compose configuration, which sets up a local cluster environment. 
It also includes Dockerfiles that enable the deployment of applications within the cluster.
- **sensors-alerts-app**: Contains the source code of the application.
- **summary-transactions-app**: Contains the source code of the application.

# Tasks description

I decided not to modify the base of the project (the initial schemas and data configurations) provided by the teacher to
make the scenario more realistic. This reflects real-world situations where we often cannot alter the data structures 
provided by the client. Therefore, all my transformations and processing are performed within the sections I developed.

## Task 1: Integration of MySQL with Kafka Connect

This is the connector I configured:

```avroschema
{
  "name": "mysql-source-connector",
  "config": {
    "connector.class": "io.confluent.connect.jdbc.JdbcSourceConnector",
    "connection.url": "jdbc:mysql://mysql:3306/db?user=user&password=password&useSSL=false",
    "mode": "timestamp",
    "table.whitelist": "sales_transactions",
    "timestamp.column.name": "timestamp",
    "poll.interval.ms": "1000",
    "tasks.max": "1",

    "key.converter": "org.apache.kafka.connect.storage.StringConverter",
    "value.converter": "io.confluent.connect.avro.AvroConverter",
    "value.converter.schema.registry.url": "http://schema-registry:8081",

    "transforms": "CastPrice, RenameTopic, SetSchemaNamespace",

    "transforms.CastPrice.type": "org.apache.kafka.connect.transforms.Cast$Value",
    "transforms.CastPrice.spec": "price:float64",

    "transforms.RenameTopic.type": "org.apache.kafka.connect.transforms.RegexRouter",
    "transforms.RenameTopic.regex": "sales_transactions",
    "transforms.RenameTopic.replacement": "sales-transactions",

    "transforms.SetSchemaNamespace.type": "org.apache.kafka.connect.transforms.SetSchemaMetadata$Value",
    "transforms.SetSchemaNamespace.schema.name": "com.farmia.sales.sales_transactions"
  }
}
```

### Process Explanation

- The connector will track the records it has already processed using the `timestamp` column defined in the table from 
which the records are being read. This approach was chosen because there is no auto-incremental column available, which
would have made the process more robust by allowing the use of a `timestamp+incremental` mode.
- The connector will poll the database every 1 second.
- I applied a cast transformation because the `price` column was causing issues. The `DOUBLE` type in SQL appears to 
behave differently, so I cast it to `float64` to ensure it could be processed correctly.
- I also implemented a topic renaming transformation to ensure records are written to the correct topic. The connector 
was reading from a table called `sales_transactions` and attempting to route the records to a topic with the same name. 
Since this topic did not exist, the connector was creating it, which is incorrect behavior. The renaming ensures that 
the records are written to the correct topic (`sales-transactions`).
- To automatically generate the SerDes classes for the Kafka stream that will process these records, I needed to define 
a namespace in the Avro schema. Initially, the connector was generating and registering a schema without a namespace in
the schema registry, causing the generated classes to mismatch the published schema. This mismatch in the schema 
registry caused my application to fail because it didn’t match the schema being written. Adding the namespace resolved 
this issue and ensured the schema was correctly mapped. 

## Task 2: Synthetic data generation using Kafka Connect

### Process Explanation
- Based on the experience got from the previous task, I defined a namespace.
- Using a regex I defined the sensor ID.
- 

## Setup

The following script automates the necessary steps to prepare the environment.

1. Starts the environment.
2. Creates the topics in the cluster.
3. Creates the transactions table in MySQL database.
4. Installs the plugins of the connectors.
5. Copy the drivers JDBC for MySQL.
6. Copy the AVRO schemas inside the connect container.
7. Starts the Kafka Streams applications.

```shell
./setup.sh
```

## Kafka Connect

The following script automates the connectors execution.

```shell
./start_connectors.sh
```

## Kafka Streams

The automation of the applications execution is made in the setup step.

## Shutdown

The following command shuts down the environment.

```shell
./shutdown.sh
```

> ⚠️ **NOTE**<br/>The state of the containers is not persisted. This means that the state and data in our cluster will be lost once we stop it.

## How to run the project?

1. Compile and package the applications by running the following command in the root folder of the project:
    ```shell
    mvn clean compile package
    ```
2. From the `target` folder of each application, copy the `{app-name}-1.0-SNAPSHOT.jar` file to the `docker/apps` 
directory, renaming it as follows:
   
    `{app-name}-1.0-SNAPSHOT.jar` → `{app-name}.jar`

3. Build the docker compose:
    ```shell
   cd docker
    docker compose build
    ```

4. Execute the setup
    ```shell
    ./setup.sh
    ```
5. Execute the connectors
    ```shell
    ./start_connectors.sh
    ```
6. Shut down the application when needed.
    ```shell
    ./shutdown.sh
    ```