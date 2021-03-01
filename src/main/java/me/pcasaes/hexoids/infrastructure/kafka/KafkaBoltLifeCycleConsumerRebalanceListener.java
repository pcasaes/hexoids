package me.pcasaes.hexoids.infrastructure.kafka;

import io.smallrye.reactive.messaging.kafka.KafkaConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.Consumer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
    public void onPartitionsAssigned(Consumer<?, ?> consumer, Collection<org.apache.kafka.common.TopicPartition> partitions) {
        long now = System.currentTimeMillis();
        long shouldStartAt = now - (this.boltMaxDuration + 10_000L);

        consumer.offsetsForTimes(partitions
                .stream()
                .collect(Collectors.toMap(k -> k, v -> shouldStartAt))
        )
                .forEach((k, v) -> {
                            LOGGER.info("Seeking to " + v);
                            consumer.seek(k, v.offset());
                            LOGGER.info("Seeked to " + v);
                        }
                );

        handler
                .onPartitionsAssigned()
                .await()
                .indefinitely();
    }


}
