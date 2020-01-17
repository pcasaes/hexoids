#!/bin/sh
set -e
echo "starting"
# If no parameter, run the game server
if [ $# -eq 0 ]; then
  exec /deployments/run-java.sh
fi

exec "$@"