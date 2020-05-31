#!/bin/sh

echo "======== CREATING TOPICS FOR HEXOIDS ========="
echo $BROKERS_LIST
echo $REPLICATION_FACTOR
echo $NUM_PARTITIONS

# BOLT_ACTION_TOPIC
bin/kafka-topics.sh --create \
  --bootstrap-server $BROKERS_LIST \
  --topic BOLT_ACTION_TOPIC \
  --replication-factor $REPLICATION_FACTOR \
  --partitions $NUM_PARTITIONS \
  --config retention.ms=60000 \
  --config segment.ms=60000 \
  --config message.timestamp.type=CreateTime \
  --config cleanup.policy=delete


# BOLT_LIFECYCLE_TOPIC
# retention/segment time cannot be less than bolt duration * 2
bin/kafka-topics.sh --create \
  --bootstrap-server $BROKERS_LIST \
  --topic BOLT_LIFECYCLE_TOPIC \
  --replication-factor $REPLICATION_FACTOR \
  --partitions $NUM_PARTITIONS \
  --config retention.ms=60000 \
  --config segment.ms=60000 \
  --config message.timestamp.type=LogAppendTime \
  --config cleanup.policy=delete


# JOIN_GAME_TOPIC
# retention 24 hours
bin/kafka-topics.sh --create \
  --bootstrap-server $BROKERS_LIST \
  --topic JOIN_GAME_TOPIC \
  --replication-factor $REPLICATION_FACTOR \
  --partitions $NUM_PARTITIONS \
  --config delete.retention.ms=1000 \
  --config min.cleanable.dirty.ratio=0.25 \
  --config retention.ms=86400000 \
  --config segment.ms=60000 \
  --config message.timestamp.type=LogAppendTime \
  --config cleanup.policy=compact,delete


# PLAYER_ACTION_TOPIC
bin/kafka-topics.sh --create \
  --bootstrap-server $BROKERS_LIST \
  --topic PLAYER_ACTION_TOPIC \
  --replication-factor $REPLICATION_FACTOR \
  --partitions $NUM_PARTITIONS \
  --config delete.retention.ms=1000 \
  --config min.cleanable.dirty.ratio=0.25 \
  --config retention.ms=600000 \
  --config segment.ms=60000 \
  --config message.timestamp.type=CreateTime \
  --config cleanup.policy=compact,delete


# SCORE_BOARD_CONTROL_TOPIC
bin/kafka-topics.sh --create \
  --bootstrap-server $BROKERS_LIST \
  --topic SCORE_BOARD_CONTROL_TOPIC \
  --replication-factor $REPLICATION_FACTOR \
  --partitions $NUM_PARTITIONS \
  --config retention.ms=60000 \
  --config segment.ms=60000 \
  --config message.timestamp.type=LogAppendTime \
  --config cleanup.policy=delete

# SCORE_BOARD_UPDATE_TOPIC
bin/kafka-topics.sh --create \
  --bootstrap-server $BROKERS_LIST \
  --topic SCORE_BOARD_UPDATE_TOPIC \
  --replication-factor $REPLICATION_FACTOR \
  --partitions $NUM_PARTITIONS \
  --config retention.ms=60000 \
  --config segment.ms=60000 \
  --config message.timestamp.type=CreateTime \
  --config cleanup.policy=delete


echo "Finished. Should exit normally"