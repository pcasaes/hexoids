package me.pcasaes.bbop.service.eventqueue;

import me.pcasaes.bbop.model.DomainEvent;
import me.pcasaes.bbop.model.Game;
import me.pcasaes.bbop.service.ConfigurationService;
import me.pcasaes.bbop.service.DtoProcessorService;
import me.pcasaes.bbop.service.kafka.KafkaProducerService;
import me.pcasaes.bbop.service.kafka.KafkaProducerType;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to generate domain events. Domain events are used to keep server nodes in sync
 */
@ApplicationScoped
public class DomainEventProducerService implements EventQueueConsumerService<DomainEvent>, Closeable {

    private static final Logger LOGGER = Logger.getLogger(DomainEventProducerService.class.getName());

    private ThreadService threadService;

    private KafkaProducerService producerService;

    private DtoProcessorService dtoProcessorService;

    private ConfigurationService configurationService;

    private GameLoopService.SleepDto sleepDto = null;

    private DtoProcessorService.JsonWriter jsonWriter;

    DomainEventProducerService() {
    }

    @Inject
    public DomainEventProducerService(ThreadService threadService,
                                      @KafkaProducerType(KafkaProducerType.Type.FAST) KafkaProducerService producerService,
                                      DtoProcessorService dtoProcessorService,
                                      ConfigurationService configurationService) {
        this.threadService = threadService;
        this.producerService = producerService;
        this.dtoProcessorService = dtoProcessorService;
        this.configurationService = configurationService;
        this.jsonWriter = dtoProcessorService.createJsonWriter();
    }

    @PreDestroy
    @Override
    public void close() {
        close(jsonWriter);
    }

    private void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
    }

    private String serialize(Object value) {
        if (threadService.isInGameLoop()) {
            try {
                return jsonWriter.writeValue(value);
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return dtoProcessorService.serializeToString(value);
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
            if (event.getEvent() != null && event.getEvent().getDtoType() == GameLoopService.SleepDto.DtoType.SLEEP_DTO) {
                this.sleepDto = (GameLoopService.SleepDto) event.getEvent();
            } else {
                String message = event.getEvent() == null ? null : serialize(event.getEvent());
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
