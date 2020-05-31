package me.pcasaes.hexoids.service.kafka;

import me.pcasaes.hexoids.model.DomainEvent;
import me.pcasaes.hexoids.model.GameTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import pcasaes.hexoids.proto.Event;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * This interface sets up a Kafka topic with a list of consumers.
 */
public interface TopicInfo {

    /**
     * Specified which game {@link GameTopic} this Kafka topic is linked to.
     *
     * @return
     */
    GameTopic topic();

    /**
     * List of consumers
     *
     * @return
     */
    default Collection<ConsumerInfo> consumerInfos() {
        return Collections.singletonList(topic()::consume);
    }


    interface ConsumerInfo {

        /**
         * Returns properties used to configure the consumer
         *
         * @return
         * @see ConsumerConfig
         */
        default Optional<Properties> consumerConfig() {
            return Optional.empty();
        }

        void consume(DomainEvent domainEvent);

        /**
         * Called after consume. Note that consumer is async.
         * <p>
         * This can be used to programmatically commit the offset
         *
         * @param kafkaConsumer
         * @param record
         * @see org.apache.kafka.clients.consumer.KafkaConsumer#commitSync(Map)
         */
        default void postConsume(Consumer<UUID, Event> kafkaConsumer, ConsumerRecord<UUID, Event> record) {
            //do nothing
        }
    }

}
