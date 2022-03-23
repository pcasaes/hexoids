package me.pcasaes.hexoids.infrastructure.kafka;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.smallrye.mutiny.tuples.Tuple3;
import me.pcasaes.hexoids.core.application.eventhandlers.ApplicationConsumers;
import org.apache.kafka.common.TopicPartition;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

@ApplicationScoped
public class DelayedStartConsumerHandler {

    private static final Logger LOGGER = Logger.getLogger(DelayedStartConsumerHandler.class.getName());

    private final Set<UniEmitter<? super Void>> emitters = ConcurrentHashMap.newKeySet();

    private final Map<TopicPartition, Long> lastOffsets = new ConcurrentHashMap<>();

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean caughtUp = new AtomicBoolean(false);


    @PreDestroy
    public void stop() {
        this.lastOffsets.clear();
        this.tryStartup();
    }

    public void start() {
        if (this.started.compareAndSet(false, true)) {
            this.emitters
                    .forEach(ue -> ue.complete(null));
            LOGGER.info("Started up delayed consumers");
        }
    }

    void register(Map<TopicPartition, Long> nextOffsets) {
        nextOffsets
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> entry.getValue() > 0)
                .forEach(entry -> {
                    LOGGER.info(() -> "Last offset: " + entry);
                    this.lastOffsets.put(entry.getKey(), entry.getValue() - 1);
                });

        this.tryStartup();
    }

    @Incoming("join-game-metadata")
    public void joinGameTime(Tuple3<String, Integer, Long> metadata) {
        if (!caughtUp.getPlain() && !caughtUp.get()) {
            TopicPartition topicPartition = new TopicPartition(metadata.getItem1(), metadata.getItem2());
            long last = this.lastOffsets.get(topicPartition);
            if (last <= metadata.getItem3()) {
                LOGGER.info(() -> "Reach offset " + last + " for " + topicPartition);
                this.lastOffsets.remove(topicPartition);
            }
            this.tryStartup();
        }
    }

    private void tryStartup() {
        if (this.lastOffsets.isEmpty() && this.caughtUp.compareAndSet(false, true)) {
            LOGGER.info("Starting up delayed consumers");
            this.start();
        }
    }

    public Uni<Void> onStarted() {
        return Uni
                .createFrom()
                .emitter(uniEmitter -> {
                    if (this.started.get()) {
                        uniEmitter.complete(null);
                    } else {
                        emitters.add(uniEmitter);
                    }
                });
    }


    private boolean hasStarted() {
        return this.caughtUp.getPlain() || this.caughtUp.get();
    }


    @Produces
    public ApplicationConsumers.HaveStarted getHaveStarted() {
        return this::hasStarted;
    }
}
