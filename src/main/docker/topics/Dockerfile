FROM solsson/kafka:latest

ENV BROKERS_LIST "localhost:9092"
ENV REPLICATION_FACTOR "1"
ENV NUM_PARTITIONS "1"

COPY create-topics.sh /create-topics.sh
RUN chmod +x /create-topics.sh

ENTRYPOINT [ "/create-topics.sh" ]

