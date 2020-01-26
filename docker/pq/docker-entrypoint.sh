#!/bin/bash
set -e

## jq isn't playing nice with SIGTERM so we fork it here as work around
exec cat out.pipe | jq -c -C --unbuffered '. | with_entries( select( .value != null ) )' &
exec pq kafka $2 --brokers $1 --beginning --msgtype pcasaes.bbop.proto.Event > out.pipe
