#
# Runner Image
# ------------

FROM rust:1.40.0-buster

## install jq for JSON formatting
RUN apt-get update -y
RUN apt-get install jq -y

## install protoc
ADD https://github.com/protocolbuffers/protobuf/releases/download/v3.11.2/protoc-3.11.2-linux-x86_64.zip protoc-3.11.2-linux-x86_64.zip
RUN unzip protoc-3.11.2-linux-x86_64.zip -d /usr
RUN chmod a+x /usr/bin/protoc

## compile bbop's protocol
ADD bbop.proto /bbop.proto
RUN /usr/bin/protoc -o bbop.fdset bbop.proto
RUN mkdir -p ~/.pq
RUN cp *.fdset ~/.pq/

## command line protobuf parser that works with Kafka
ADD https://github.com/sevagh/pq/releases/download/1.4.0/pq-bin.tar.gz pq-bin.tar.gz
RUN gzip -dc pq-bin.tar.gz | tar xvf -
RUN chmod a+x pq
RUN mv pq /usr/bin/pq

# setup entry point
RUN mkfifo out.pipe
ADD docker-entrypoint.sh docker-entrypoint.sh
RUN chmod a+x docker-entrypoint.sh

ENTRYPOINT ["./docker-entrypoint.sh"]

