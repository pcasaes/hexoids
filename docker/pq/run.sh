#!/bin/sh

set -e
docker run --rm --init --network=host me.pcasaes/pq $1 $2