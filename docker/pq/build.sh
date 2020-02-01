#!/bin/sh

set -e
cp ../../src/main/proto/pcasaes/hexoids/proto/hexoids.proto .

docker build -t me.pcasaes/pq .