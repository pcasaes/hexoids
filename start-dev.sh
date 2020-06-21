#!/bin/sh

docker-compose -f docker-compose-dev-infrastructure.yml build
docker-compose -f docker-compose-dev-infrastructure.yml up -d