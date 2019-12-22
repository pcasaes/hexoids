package me.paulo.casaes.bbop.service;

import me.paulo.casaes.bbop.model.DomainEvent;
import me.paulo.casaes.bbop.model.GameEvents;
import me.paulo.casaes.bbop.service.kafka.KafkaProducerService;
import me.paulo.casaes.bbop.util.concurrent.eventqueue.EventQueue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class DomainEventProducerService {

    private static final Logger LOGGER = Logger.getLogger(DomainEventProducerService.class.getName());

    private KafkaProducerService producerService;

    private DtoProcessorService dtoProcessorService;

    private ConfigurationService configurationService;

    private EventQueue<DomainEvent> eventQueue;

    private Thread thread;
    private boolean running = true;


    @Inject
    public DomainEventProducerService(KafkaProducerService producerService,
                                      DtoProcessorService dtoProcessorService,
                                      ConfigurationService configurationService) {
        this.producerService = producerService;
        this.dtoProcessorService = dtoProcessorService;
        this.configurationService = configurationService;

        GameEvents.getDomainEvents().setConsumer(this::publish);
    }

    public void startup(@Observes @Initialized(ApplicationScoped.class) Object event) {
        LOGGER.info("Eager load " + ClientBroadcastService.class.getName());
    }

    @PostConstruct
    public void start() {
        this.eventQueue = EventQueue.Factory.createSingleProducerSingleConsumerEventQueue(
                configurationService.isClientBroadcastUseLinkedList(),
                configurationService.getClientBroadcastMaxSizeExponent());
        thread = new Thread(this::run);
        thread.setName(DomainEventProducerService.class.getSimpleName());
        thread.setDaemon(true);
        thread.start();
    }

    private void publish(DomainEvent domainEvent) {
        eventQueue.produce(domainEvent);
    }


    private void run() {
        while (running) {
            DomainEvent event;
            while ((event = eventQueue.consume()) != null) {
                try {
                    String message = event.getEvent() == null ? null : dtoProcessorService.serializeToString(event.getEvent());
                    this.producerService.send(event.getTopic(), event.getKey(), message, false);
                } catch (RuntimeException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
            sleep();
        }
    }

    private void sleep() {
        try {
            Thread.sleep(20L);
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    public void stop() {
        this.running = false;
        try {
            thread.join(5_000L);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            Thread.currentThread().interrupt();
        }
    }


}
