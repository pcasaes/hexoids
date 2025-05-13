package me.pcasaes.hexoids.configuration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.pcasaes.hexoids.entrypoints.jobs.periodictasks.FlushClientBroadcasterPeriodicTask;
import me.pcasaes.hexoids.infrastructure.disruptor.DisruptorIn;


@ApplicationScoped
public class FlushClientBroadcasterPeriodicTaskClientQueueProvider {

    private final DisruptorIn disruptorIn;

    @Inject
    public FlushClientBroadcasterPeriodicTaskClientQueueProvider(DisruptorIn disruptorIn) {
        this.disruptorIn = disruptorIn;
    }

    @Produces
    @Singleton
    public FlushClientBroadcasterPeriodicTask.ClientQueue getClientQueue() {
        return disruptorIn::enqueueClient;
    }
}
