#!/bin/sh

# KRaft required step: Format the storage directory with a new cluster ID
sed -i '/\. \/etc\/confluent\/docker\/bash-config/a\
if [[ -z "${CLUSTER_ID:-}" ]]; then\n  export CLUSTER_ID=`kafka-storage random-uuid`\nfi' /etc/confluent/docker/run