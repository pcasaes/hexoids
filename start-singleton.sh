#!/bin/sh

chmod -R a+rw src/main/monitoring/grafana
docker-compose -f docker-compose-singleton.yml up -d