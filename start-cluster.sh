#!/bin/sh

chmod -R a+rw src/main/monitoring/grafana

docker compose -f docker-compose.yml build
docker compose -f docker-compose.yml up -d