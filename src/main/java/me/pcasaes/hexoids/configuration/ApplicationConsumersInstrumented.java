package me.pcasaes.hexoids.configuration;

import me.pcasaes.hexoids.configuration.metrics.ApplicationConsumerMetrics;
import me.pcasaes.hexoids.core.application.eventhandlers.ApplicationConsumers;
import me.pcasaes.hexoids.core.application.eventhandlers.ApplicationConsumersImpl;
import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue;
import me.pcasaes.hexoids.core.domain.model.DomainEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
@ApplicationConsumerMetrics
public class ApplicationConsumersInstrumented implements ApplicationConsumers {


    private final ApplicationConsumers delegate;

    public ApplicationConsumersInstrumented() {
        this.delegate = null;
    }

    @Inject
    public ApplicationConsumersInstrumented(GameQueue gameQueue) {
        this.delegate = ApplicationConsumersImpl.create(gameQueue);
    }

    @Override
    public void onJoinGame(DomainEvent domainEvent) {
        delegate.onJoinGame(domainEvent);
    }

    @Override
    public void onPlayerAction(DomainEvent domainEvent) {
        delegate.onPlayerAction(domainEvent);
    }

    @Override
    public void onBoltLifeCycle(DomainEvent domainEvent) {
        delegate.onBoltLifeCycle(domainEvent);
    }

    @Override
    public void onBoltAction(DomainEvent domainEvent) {
        delegate.onBoltAction(domainEvent);
    }

    @Override
    public void onScoreBoardControl(DomainEvent domainEvent) {
        delegate.onScoreBoardControl(domainEvent);
    }

    @Override
    public void onScoreBoardUpdate(DomainEvent domainEvent) {
        delegate.onScoreBoardUpdate(domainEvent);
    }





}
