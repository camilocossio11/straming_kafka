#!/bin/bash

echo "Compiling and packaging applications..."
mvn clean compile package

echo "Copying .jar files to its corresponding location..."
cp ./sensors-alerts-app/target/sensors-alerts-app-1.0-SNAPSHOT.jar ./docker/apps/sensors-alerts-app.jar
cp ./summary-transactions-app/target/sales-summary-app-1.0-SNAPSHOT.jar ./docker/apps/sales-summary-app.jar

echo "Building docker compose..."
cd ./docker
docker compose build