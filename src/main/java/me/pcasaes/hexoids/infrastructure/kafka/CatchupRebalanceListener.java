package me.pcasaes.hexoids.infrastructure.kafka;

import io.smallrye.reactive.messaging.kafka.KafkaConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
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
