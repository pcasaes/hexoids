package me.paulo.casaes.bbop.service;

import me.paulo.casaes.bbop.model.DomainEvent;
import me.paulo.casaes.bbop.model.GameEvents;
import me.paulo.casaes.bbop.service.eventqueue.ClientBroadcastService;
import me.paulo.casaes.bbop.service.eventqueue.EventQueueService;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.logging.Logger;

/**
 * Used to initialize the model and wire it into the services.
 */
@ApplicationScoped
public class ModelInitializer {

    private static final Logger LOGGER = Logger.getLogger(ModelInitializer.class.getName());

    private EventQueueService<DomainEvent> domainEventProducerService;
    private EventQueueService<ClientBroadcastService.ClientEvent> clientBroadcastService;

    @Inject
    public ModelInitializer(
            EventQueueService<DomainEvent> domainEventProducerService,
            EventQueueService<ClientBroadcastService.ClientEvent> clientBroadcastService) {
        this.domainEventProducerService = domainEventProducerService;
        this.clientBroadcastService = clientBroadcastService;
    }


    public void startup(@Observes @Initialized(ApplicationScoped.class) Object event) {
        LOGGER.info("Eager load " + this.getClass().getName());
    }

    @PostConstruct
    public void start() {
        GameEvents.getDomainEvents().setConsumer(domainEventProducerService::enqueue);
        GameEvents.getClientEvents().setConsumer(dto -> clientBroadcastService.enqueue(ClientBroadcastService.ClientEvent.of(dto)));
    }
}
