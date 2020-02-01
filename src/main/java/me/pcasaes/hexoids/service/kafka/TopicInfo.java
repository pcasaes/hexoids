package me.pcasaes.hexoids.service.kafka;

import me.pcasaes.hexoids.model.DomainEvent;
import me.pcasaes.hexoids.model.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import pcasaes.hexoids.proto.Event;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

public interface TopicInfo {

    NewTopic newTopic();

    Topics topic();

    default Collection<ConsumerInfo> consumerInfos() {
        return Collections.singletonList(topic()::consume);
    }


    interface ConsumerInfo {

        default boolean useSubscription() {
            return false;
        }

        default Optional<Properties> consumerConfig() {
            return Optional.empty();
        }

        void consume(DomainEvent domainEvent);

        /**
         * Called after consume. Note that consumer is async.
         *
         * @param kafkaConsumer
         * @param record
         */
        default void postConsume(Consumer<UUID, Event> kafkaConsumer, ConsumerRecord<UUID, Event> record) {
            //do nothing
        }
    }

}
