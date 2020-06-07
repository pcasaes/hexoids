package me.pcasaes.hexoids.infrastructure.periodictasks;

import io.quarkus.runtime.StartupEvent;
import me.pcasaes.hexoids.infrastructure.broadcaster.ClientBroadcaster;
import me.pcasaes.hexoids.infrastructure.disruptor.DisruptorIn;
import pcasaes.hexoids.proto.Dto;
import pcasaes.hexoids.proto.Flush;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.interceptor.Interceptor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class FlushClientBroadcasterPeriodicTask {

    private static final Dto FLUSH = Dto
            .newBuilder()
            .setFlush(Flush.newBuilder())
            .build();

    private final DisruptorIn disruptorIn;
    private final ClientBroadcaster clientBroadcaster;

    private volatile ScheduledExecutorService scheduledExecutorService;


    @Inject
    public FlushClientBroadcasterPeriodicTask(DisruptorIn disruptorIn,
                                              ClientBroadcaster clientBroadcaster) {
        this.disruptorIn = disruptorIn;
        this.clientBroadcaster = clientBroadcaster;
    }

    public void startup(@Observes @Priority(Interceptor.Priority.APPLICATION + 900) StartupEvent event) {
        //eager load
    }


    @PostConstruct
    public void start() {
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();


        scheduledExecutorService.scheduleAtFixedRate(
                this::clientFlushPeriodicTask,
                1000,
                this.clientBroadcaster.getBatchTimeout(),
                TimeUnit.MILLISECONDS);
    }

    private void clientFlushPeriodicTask() {
        if (clientBroadcaster.canFlush()) {
            flush();
        }
    }


    private void flush() {
        disruptorIn.enqueueClient(FLUSH);
    }

    @PreDestroy
    public void stop() {
        scheduledExecutorService.shutdown();
    }
}
