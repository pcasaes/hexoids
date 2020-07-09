package me.pcasaes.hexoids.infrastructure.kafka;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.KafkaConsumerRebalanceListener;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Set;

@ApplicationScoped
@Named("DelayedStartConsumerRebalanceListener")
public class DelayedStartConsumerRebalanceListener implements KafkaConsumerRebalanceListener {

    private final DelayedStartConsumerHandler handler;

    @Inject
    public DelayedStartConsumerRebalanceListener(DelayedStartConsumerHandler handler) {
        this.handler = handler;
    }

    @Override
    public Uni<Void> onPartitionsAssigned(KafkaConsumer<?, ?> consumer, Set<TopicPartition> topicPartitions) {
        return this.handler.onPartitionsAssigned();
    }

    @Override
    public Uni<Void> onPartitionsRevoked(KafkaConsumer<?, ?> kafkaConsumer, Set<TopicPartition> set) {
        return Uni
                .createFrom()
                .nullItem();
    }
}
