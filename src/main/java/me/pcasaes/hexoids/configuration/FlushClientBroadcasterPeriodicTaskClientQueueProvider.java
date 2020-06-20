package me.pcasaes.hexoids.configuration;

import me.pcasaes.hexoids.entrypoints.jobs.periodictasks.FlushClientBroadcasterPeriodicTask;
import me.pcasaes.hexoids.infrastructure.disruptor.DisruptorIn;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

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
