package me.paulo.casaes.bbop.service;

import me.paulo.casaes.bbop.util.concurrent.eventqueue.EventQueue;
import me.paulo.casaes.bbop.dto.DirectedCommandDto;
import me.paulo.casaes.bbop.dto.Dto;
import me.paulo.casaes.bbop.dto.EventDto;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class ClientBroadcastService {

    private static final Logger LOGGER = Logger.getLogger(ClientBroadcastService.class.getName());

    private final EventQueue<Dto> eventQueue = EventQueue.Factory.createSingleProducerSingleConsumerEventQueue();

    private final SessionService sessionService;
    private final DtoProcessorService dtoProcessorService;

    ClientBroadcastService() {
        this.sessionService = null;
        this.dtoProcessorService = null;
    }

    @Inject
    public ClientBroadcastService(SessionService sessionService, DtoProcessorService dtoProcessorService) {
        this.sessionService = sessionService;
        this.dtoProcessorService = dtoProcessorService;
    }

    private Thread thread;
    private boolean running = true;

    void broadcast(Dto event) {
        eventQueue.produce(event);
    }

    @PostConstruct
    public void start() {
        thread = new Thread(this::run);
        thread.setName(GameLoopService.class.getSimpleName());
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
        }
    }
}
