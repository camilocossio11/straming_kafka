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