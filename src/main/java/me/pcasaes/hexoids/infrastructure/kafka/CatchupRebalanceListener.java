package me.pcasaes.hexoids.infrastructure.kafka;

import io.smallrye.reactive.messaging.kafka.KafkaConsumerRebalanceListener;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.Collection;

@ApplicationScoped
@Named("CatchupRebalanceListener")
public class CatchupRebalanceListener implements KafkaConsumerRebalanceListener {

    @Inject
    DelayedStartConsumerHandler delayedStartConsumerHandler;

    @Override
    public void onPartitionsAssigned(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
        delayedStartConsumerHandler.register(consumer.endOffsets(partitions, Duration.ofSeconds(30)));
    }
}
