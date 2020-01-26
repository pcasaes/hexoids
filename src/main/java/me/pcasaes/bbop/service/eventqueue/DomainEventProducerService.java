package me.pcasaes.bbop.service.eventqueue;

import com.google.protobuf.GeneratedMessageLite;
import me.pcasaes.bbop.model.DomainEvent;
import me.pcasaes.bbop.model.Game;
import me.pcasaes.bbop.service.ConfigurationService;
import me.pcasaes.bbop.service.kafka.KafkaProducerService;
import me.pcasaes.bbop.service.kafka.KafkaProducerType;
import pcasaes.bbop.proto.Sleep;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Used to generate domain events. Domain events are used to keep server nodes in sync
 */
@ApplicationScoped
public class DomainEventProducerService implements EventQueueConsumerService<DomainEvent> {

    private ThreadService threadService;

    private KafkaProducerService producerService;

    private ConfigurationService configurationService;

    private Sleep sleepDto = null;

    DomainEventProducerService() {
    }

    @Inject
    public DomainEventProducerService(ThreadService threadService,
                                      @KafkaProducerType(KafkaProducerType.Type.FAST) KafkaProducerService producerService,
                                      ConfigurationService configurationService) {
        this.threadService = threadService;
        this.producerService = producerService;
        this.configurationService = configurationService;
    }

    private byte[] serialize(GeneratedMessageLite<?, ?> value) {
        return value.toByteArray();
    }

    @Override
    public boolean bypassEnqueue(DomainEvent event) {
        if (threadService.isInGameLoop()) {
            return false;
        }
        accept(event);
        return true;
    }

    @Override
    public void accept(DomainEvent event) {
        if (event != null) {
            if (event.getEvent() != null && event.getEvent().hasSleep()) {
                this.sleepDto = event.getEvent().getSleep();
            } else {
                byte[] message = event.getEvent() == null ? null : serialize(event.getEvent());
                this.producerService.send(event.getTopic(), event.getKey(), message);
            }
        }
    }

    @Override
    public void empty() {
        //do nothing on empty
    }

    @Override
    public long getWaitTime() {
        if (this.sleepDto == null) {
            return 0L;
        }
        long waitTime = sleepDto.getSleepUntil() - Game.get().getClock().getTime();
        this.sleepDto = null;

        return waitTime;
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
