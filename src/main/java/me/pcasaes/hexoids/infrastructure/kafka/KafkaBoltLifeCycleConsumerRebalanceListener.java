package me.pcasaes.hexoids.infrastructure.kafka;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.KafkaConsumerRebalanceListener;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

@ApplicationScoped
@Named("hexoids-server-bolt-life-cycle")
public class KafkaBoltLifeCycleConsumerRebalanceListener implements KafkaConsumerRebalanceListener {

    private static final Logger LOGGER = Logger.getLogger(KafkaBoltLifeCycleConsumerRebalanceListener.class.getName());

    private final DelayedStartConsumerHandler handler;
    private final int boltMaxDuration;

    @Inject
    public KafkaBoltLifeCycleConsumerRebalanceListener(
            DelayedStartConsumerHandler handler,
            @ConfigProperty(
                    name = "hexoids.config.bolt.max.duration",
                    defaultValue = "10000"
            ) int boltMaxDuration) {
        this.handler = handler;
        this.boltMaxDuration = boltMaxDuration;
    }

    @Override
    public Uni<Void> onPartitionsAssigned(KafkaConsumer<?, ?> consumer, Set<TopicPartition> set) {
        long now = System.currentTimeMillis();
        long shouldStartAt = now - (this.boltMaxDuration + 10_000L);

        List<Uni<Void>> unis = new ArrayList<>();
        set
                .stream()
                .map(topicPartition -> {
                    LOGGER.info("Assigned " + topicPartition);
                    return consumer.offsetsForTimes(topicPartition, shouldStartAt)
                            .onItem()
                            .invoke(o -> LOGGER.info("Seeking to " + o))
                            .onItem()
                            .ifNotNull()
                            .produceUni(o -> consumer
                                    .seek(topicPartition, o.getOffset())
                                    .onItem()
                                    .invoke(v -> LOGGER.info("Seeked to " + o))
                            );
                })
                .forEach(unis::add);

        unis.add(handler.onPartitionsAssigned());

        return Uni
                .combine()
                .all()
                .unis(unis)
                .combinedWith(a -> null);
    }

    @Override
    public Uni<Void> onPartitionsRevoked(KafkaConsumer<?, ?> kafkaConsumer, Set<TopicPartition> set) {
        return Uni
                .createFrom()
                .nullItem();
    }

}
