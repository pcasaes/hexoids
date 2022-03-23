#!/bin/sh
set -e
echo "starting"
export KAFKA_GROUP_ID=hexoids-$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)
echo "kafka group.id ${KAFKA_GROUP_ID}"
# If no parameter, run the service
if [ $# -eq 0 ]; then
  if [ -z $JAVA_EXTRA_OPTS ]; then
    echo "Do not modify JAVA_OPTIONS"
  else
    JAVA_OPTIONS="${JAVA_OPTIONS} ${JAVA_EXTRA_OPTS}"
  fi
  JAVA_OPTIONS="${JAVA_OPTIONS} -Dquarkus.vertx.cluster.host=${HOSTNAME}"
  echo "JAVA_OPTIONS: ${JAVA_OPTIONS}"
  exec /deployments/run-java.sh
fi

exec "$@"