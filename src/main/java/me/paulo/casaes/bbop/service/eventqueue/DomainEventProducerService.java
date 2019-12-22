package me.paulo.casaes.bbop.service.eventqueue;

import me.paulo.casaes.bbop.model.DomainEvent;
import me.paulo.casaes.bbop.service.ConfigurationService;
import me.paulo.casaes.bbop.service.DtoProcessorService;
import me.paulo.casaes.bbop.service.kafka.KafkaProducerService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Used to generate domain events. Domain events are used to keep server nodes in sync
 */
@ApplicationScoped
public class DomainEventProducerService implements EventQueueConsumerService<DomainEvent> {

    private KafkaProducerService producerService;

    private DtoProcessorService dtoProcessorService;

    private ConfigurationService configurationService;


    DomainEventProducerService() {
    }

    @Inject
    public DomainEventProducerService(KafkaProducerService producerService,
                                      DtoProcessorService dtoProcessorService,
                                      ConfigurationService configurationService) {
        this.producerService = producerService;
        this.dtoProcessorService = dtoProcessorService;
        this.configurationService = configurationService;
    }

    @Override
    public void accept(DomainEvent event) {
        if (event != null) {
            String message = event.getEvent() == null ? null : dtoProcessorService.serializeToString(event.getEvent());
            this.producerService.send(event.getTopic(), event.getKey(), message, false);
        }
    }

    @Override
    public void empty() {
        //do nothing on empty
    }

    @Override
    public long getWaitTime() {
        return 20L;
    }

    @Override
    public boolean useLinkedList() {
        return configurationService.isDomainEventUseLinkedList();
    }

    @Override
    public boolean useSingleProducer() {
        return true;
    }

    @Override
    public int getMaxSizeExponent() {
        return configurationService.getDomainEventMaxSizeExponent();
    }

    @Override
    public String getName() {
        return DomainEventProducerService.class.getSimpleName();
    }

    @Override
    public Class<?> getEventType() {
        return DomainEvent.class;
    }
}
