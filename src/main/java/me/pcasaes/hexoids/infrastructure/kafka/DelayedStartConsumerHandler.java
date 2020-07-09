package me.pcasaes.hexoids.infrastructure.kafka;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import me.pcasaes.hexoids.core.application.eventhandlers.ApplicationConsumers;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
public class DelayedStartConsumerHandler {

    private final Set<UniEmitter<? super Void>> emitters = ConcurrentHashMap.newKeySet();

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean caughtUp = new AtomicBoolean(false);

    private volatile long startedAt;
    private volatile long lastCheck;


    @PostConstruct
    public void startup() {

        long now = System.currentTimeMillis();
        this.startedAt = now;
        this.lastCheck = now + 5_000L;

        final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
                    if (this.caughtUp.get() || System.currentTimeMillis() > this.lastCheck) {
                        this.start();
                        this.caughtUp.set(true);
                        scheduledExecutorService.shutdown();
                    }
                },
                1,
                1,
                TimeUnit.SECONDS);
    }


    @PreDestroy
    public void stop() {
        this.caughtUp.set(true);
    }

    public void start() {
        if (this.started.compareAndSet(false, true)) {
            this.emitters
                    .forEach(ue -> ue.complete(null));
        }
    }

    @Incoming("join-game-time")
    public void joinGameTime(long time) {
        if (!caughtUp.getPlain() && !caughtUp.get()) {
            if (time >= this.startedAt) {
                caughtUp.set(true);
                this.start();
            } else {
                lastCheck = System.currentTimeMillis() + 1000L;
            }
        }
    }

    public Uni<Void> onPartitionsAssigned() {
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
        if (this.caughtUp.getPlain()) {
            return true;
        }
        if (!this.caughtUp.get() || this.lastCheck <= System.currentTimeMillis()) {
            this.caughtUp.set(true);
        }
        return this.caughtUp.get();
    }


    @Produces
    public ApplicationConsumers.HaveStarted getHaveStarted() {
        return this::hasStarted;
    }
}
