#!/bin/sh

set -e
cp ../../src/main/proto/pcasaes/bbop/proto/bbop.proto .

docker build -t me.pcasaes/pq .