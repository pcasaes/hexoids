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
  echo "JAVA_OPTIONS: ${JAVA_OPTIONS}"
  exec /deployments/run-java.sh
fi

exec "$@"