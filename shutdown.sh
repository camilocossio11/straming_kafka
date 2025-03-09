#/bin/bash

cd ./docker

echo "Stopping environment"
docker compose down --remove-orphans
echo "OK"
