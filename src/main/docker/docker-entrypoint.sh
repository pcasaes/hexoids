#!/bin/sh
set -e
echo "starting"

# If no parameter, run the game server
if [ $# -eq 0 ]; then
  if [ -z $JAVA_EXTRA_OPTS ]; then
    echo "Do not modify JAVA_OPTIONS"
  else
    JAVA_OPTIONS="${JAVA_OPTIONS} ${JAVA_EXTRA_OPTS}"
  fi
  IP_ADDRESS=`hostname -i`
  JAVA_OPTIONS="${JAVA_OPTIONS} -Dquarkus.http.domain-socket-enabled=true -Dquarkus.http.domain-socket=/deployments/sockets/${IP_ADDRESS}.socket"
  echo "JAVA_OPTIONS: ${JAVA_OPTIONS}"
  exec /deployments/run-java.sh
fi

exec "$@"