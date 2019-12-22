package me.paulo.casaes.bbop.service;

import me.paulo.casaes.bbop.dto.DirectedCommandDto;
import me.paulo.casaes.bbop.dto.Dto;
import me.paulo.casaes.bbop.dto.EventDto;
import me.paulo.casaes.bbop.model.GameEvents;
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
public class ClientBroadcastService {

    private static final Logger LOGGER = Logger.getLogger(ClientBroadcastService.class.getName());

    private EventQueue<Dto> eventQueue;

    private final SessionService sessionService;
    private final DtoProcessorService dtoProcessorService;
    private final ConfigurationService configurationService;

    ClientBroadcastService() {
        this.sessionService = null;
        this.dtoProcessorService = null;
        this.configurationService = null;
        this.eventQueue = null;
    }

    @Inject
    public ClientBroadcastService(SessionService sessionService,
                                  DtoProcessorService dtoProcessorService,
                                  ConfigurationService configurationService) {
        this.sessionService = sessionService;
        this.dtoProcessorService = dtoProcessorService;
        this.configurationService = configurationService;

        GameEvents.getClientEvents().setConsumer(this::broadcast);
    }

    private Thread thread;
    private boolean running = true;

    private void broadcast(Dto event) {
        eventQueue.produce(event);
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
        thread.setName(ClientBroadcastService.class.getSimpleName());
        thread.setDaemon(true);
        thread.start();
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


    private void run() {
        while (running) {
            Dto dto;
            while ((dto = eventQueue.consume()) != null) {
                try {
                    if (dto instanceof EventDto) {
                        this.sessionService.broadcast(dtoProcessorService.serializeToString(dto));
                    } else {
                        DirectedCommandDto command = (DirectedCommandDto) dto;
                        this.sessionService.direct(command.getPlayerId(), dtoProcessorService.serializeToString(command.getCommand()));
                    }
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
}
