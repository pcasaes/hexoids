package me.pcasaes.hexoids.service.eventqueue;

import me.pcasaes.hexoids.util.concurrent.eventqueue.EventQueue;

import java.util.logging.Level;
import java.util.logging.Logger;

public class EventQueueService<T> {

    private static final Logger LOGGER = Logger.getLogger(EventQueueService.class.getName());

    private EventQueue<T> eventQueue;

    private Thread thread;
    private boolean running = true;
    private EventQueueConsumerService<T> eventQueueExecutorService;
    private final QueueMetric metric;


    public void enqueue(T event) {
        if (eventQueueExecutorService.isEnabled() && !eventQueueExecutorService.bypassEnqueue(event)) {
            eventQueue.produce(event);
        }
    }


    EventQueueService(EventQueueConsumerService<T> eventQueueExecutorService, QueueMetric metric) {
        this.eventQueueExecutorService = eventQueueExecutorService;
        this.metric = metric;
        this.metric.setName(eventQueueExecutorService.getName());

    }

    public void start() {
        if (eventQueueExecutorService.isEnabled()) {
            if (eventQueueExecutorService.useSingleProducer()) {
                this.eventQueue = EventQueue.Factory.createSingleProducerSingleConsumerEventQueue(
                        eventQueueExecutorService.useLinkedList(),
                        eventQueueExecutorService.getMaxSizeExponent()
                );
            } else {
                this.eventQueue = EventQueue.Factory.createMultipleProducerSingleConsumerEventQueue(
                        eventQueueExecutorService.useLinkedList(),
                        eventQueueExecutorService.getMaxSizeExponent()
                );
            }

            this.thread = new Thread(this::run);
            thread.setName(this.eventQueueExecutorService.getName());
            thread.setDaemon(true);
            thread.start();
        }
    }

    void destroy() {
        this.running = false;
        if (thread != null) {
            try {
                thread.join(5_000L);
            } catch (InterruptedException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                Thread.currentThread().interrupt();
            }
        }
    }


    private void run() {
        this.eventQueueExecutorService.executePreLoopTasks();
        this.metric.running();
        while (running) {
            T event;
            while ((event = eventQueue.consume()) != null) {
                this.metric.calculateLoadFactor();
                try {
                    this.eventQueueExecutorService.accept(event);
                } catch (RuntimeException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
            this.eventQueueExecutorService.empty();
            sleep();
        }
    }

    private void sleep() {
        this.metric.calculateLoadFactor();
        long time = this.eventQueueExecutorService.getWaitTime();
        if (time > 0L) {
            try {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Sleeping " + time + " for event queue " + eventQueueExecutorService.getName());
                }
                this.metric.sleeping();
                Thread.sleep(time);
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
            this.metric.running();
        }
    }
}
