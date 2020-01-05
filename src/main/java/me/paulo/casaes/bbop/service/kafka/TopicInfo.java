package me.paulo.casaes.bbop.service.kafka;

import me.paulo.casaes.bbop.dto.EventDto;
import me.paulo.casaes.bbop.model.DomainEvent;
import me.paulo.casaes.bbop.model.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;

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
         * If the consumer does not use autocommit will commit on return true
         * @param record
         * @return
         */
        default boolean consumeRecord(ConsumerRecord<UUID, EventDto> record) {
            if (record.value() == null) {
                this.consume(DomainEvent.deleted(record.key()));
            } else {
                this.consume(DomainEvent.of(record.key(), record.value()));
            }
            return false;
        }
    }

}
