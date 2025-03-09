
# Entorno

## Descripción

The local environment is based on a Docker 🐳 with the following containers:

```             
controller-1
controller-2
controller-3
broker-1
broker-2
broker-3
connect
control-center
ksqldb-cli
ksqldb-server
schema-registry
mysql     
```

## Comandos

To initialize the environment, run the following command from within the **docker** directory:

* Start the environment:

```bash
docker compose up -d
```

* Verify the containers status:

```bash
docker compose ps
```

* Stop the environment:

```bash
docker compose down
```

## URLs

* Control Center : http://localhost:9021
* Schema Registry: http://localhost:8081
* Kafka Connect: http://localhost:8083
* ksqlDB: http://localhost:8088


> ⚠️ **NOTE**<br/>The state of the containers is not persisted. This means that the state and data in our cluster will be lost once we stop it. 