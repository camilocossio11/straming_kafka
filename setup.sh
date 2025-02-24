#/bin/bash

cd ./docker

echo "Starting Environment"
docker compose up -d
sleep 30

echo "Creating sensor-telemetry topic..."
docker exec -it broker-1 bash
kafka-topics --create --bootstrap-server broker-1:29092 --topic sensor-telemetry --partitions 3 --replication-factor 2
kafka-topics --create --bootstrap-server broker-1:29092 --topic sales-transactions --partitions 3 --replication-factor 2
kafka-topics --create --bootstrap-server broker-1:29092 --topic sensor-alerts --partitions 3 --replication-factor 2
kafka-topics --create --bootstrap-server broker-1:29092 --topic sales-summary --partitions 3 --replication-factor 2

echo "Creating transactions table in MySQL"
docker cp ../sql/transactions.sql mysql:/
docker exec mysql bash -c "mysql --user=root --password=password --database=db < /transactions.sql"

echo "Installing connectors..."
docker compose exec connect confluent-hub install --no-prompt confluentinc/kafka-connect-datagen:latest
docker compose exec connect confluent-hub install --no-prompt confluentinc/kafka-connect-jdbc:latest

echo "Copying drivers MySQL..."
docker cp ../sql/mysql/mysql-connector-java-5.1.45.jar connect:/usr/share/confluent-hub-components/confluentinc-kafka-connect-jdbc/lib/mysql-connector-java-5.1.45.jar

echo "Copying schemas AVRO..."
docker cp ../datagen/sensor-telemetry.avsc connect:/home/appuser/
docker cp ../datagen/transactions.avsc connect:/home/appuser/

echo "Restarting connect container..."
docker compose restart connect
echo "Waiting for restarting..."
sleep 30

echo "OK"
