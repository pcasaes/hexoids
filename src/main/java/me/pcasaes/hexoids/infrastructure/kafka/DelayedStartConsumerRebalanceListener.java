package me.pcasaes.hexoids.infrastructure.kafka;

import io.smallrye.reactive.messaging.kafka.KafkaConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.Consumer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;

@ApplicationScoped
@Named("DelayedStartConsumerRebalanceListener")
public class DelayedStartConsumerRebalanceListener implements KafkaConsumerRebalanceListener {

    private final DelayedStartConsumerHandler handler;

    @Inject
    public DelayedStartConsumerRebalanceListener(DelayedStartConsumerHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onPartitionsAssigned(Consumer<?, ?> consumer, Collection<org.apache.kafka.common.TopicPartition> partitions) {
        this.handler
                .onStarted()
                .await()
                .indefinitely();
    }

}
