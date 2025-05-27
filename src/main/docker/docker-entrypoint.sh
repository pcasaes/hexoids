#!/bin/sh
set -e
echo "starting"
# If no parameter, run the service
if [ $# -eq 0 ]; then
  if [ -z $JAVA_EXTRA_OPTS ]; then
    echo "Do not modify JAVA_OPTS"
  else
    JAVA_OPTS="${JAVA_OPTS} ${JAVA_EXTRA_OPTS}"
  fi
  JAVA_OPTS="${JAVA_OPTS} -Dquarkus.vertx.cluster.host=${HOSTNAME}"
  echo "JAVA_OPTS: ${JAVA_OPTS}"
  exec /opt/jboss/container/java/run/run-java.sh
fi

exec "$@"
