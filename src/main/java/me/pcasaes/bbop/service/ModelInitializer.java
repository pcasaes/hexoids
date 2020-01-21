package me.pcasaes.bbop.service;

import io.quarkus.runtime.StartupEvent;
import me.pcasaes.bbop.model.DomainEvent;
import me.pcasaes.bbop.model.GameEvents;
import me.pcasaes.bbop.service.eventqueue.ClientBroadcastService;
import me.pcasaes.bbop.service.eventqueue.EventQueueService;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
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


    public void startup(@Observes StartupEvent event) {
        LOGGER.info("Eager load " + this.getClass().getName());
    }

    @PostConstruct
    public void start() {
        GameEvents.getDomainEvents().setConsumer(domainEventProducerService::enqueue);
        GameEvents.getClientEvents().setConsumer(dto -> clientBroadcastService.enqueue(ClientBroadcastService.ClientEvent.of(dto)));
    }
}
