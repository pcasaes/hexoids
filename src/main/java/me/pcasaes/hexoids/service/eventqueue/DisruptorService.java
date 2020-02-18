package me.pcasaes.hexoids.service.eventqueue;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.dsl.ProducerType;
import io.quarkus.scheduler.Scheduled;
import me.pcasaes.hexoids.model.DomainEvent;
import me.pcasaes.hexoids.model.GameEvents;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pcasaes.hexoids.proto.Dto;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@ApplicationScoped
public class DisruptorService {

    private static final Consumer<Dto> CLIENT_EVENT_NOOP = v -> {
    };

    private static final Consumer<DomainEvent> DOMAIN_EVENT_NOOP = v -> {
    };

    private final EventQueueConsumerService<GameLoopService.GameRunnable> gameRunnableEventQueueConsumerService;
    private final EventQueueConsumerService<DomainEvent> domainEventEventQueueConsumerService;
    private final EventQueueConsumerService<Dto> clientEventEventQueueConsumerService;
    private final int bufferSizeExponent;
    private final List<QueueMetric> metrics;

    private final EventTranslatorOneArg<DisruptorEvent, GameLoopService.GameRunnable> gameTranslator = this::translate;
    private final EventTranslatorOneArg<DisruptorEvent, Dto> dtoTranslator = this::translate;

    private RingBuffer<DisruptorEvent> ringBuffer;
    private Disruptor<DisruptorEvent> disruptor;

    public DisruptorService() {
        this.gameRunnableEventQueueConsumerService = null;
        this.domainEventEventQueueConsumerService = null;
        this.clientEventEventQueueConsumerService = null;
        this.metrics = null;
        this.bufferSizeExponent = 0;
    }

    @Inject
    public DisruptorService(
            EventQueueConsumerService<GameLoopService.GameRunnable> gameRunnableEventQueueConsumerService,
            EventQueueConsumerService<DomainEvent> domainEventEventQueueConsumerService,
            EventQueueConsumerService<Dto> clientEventEventQueueConsumerService,
            @ConfigProperty(
                    name = "hexoids.config.service.disruptor.buffer-size-exponent",
                    defaultValue = "17"
            ) int bufferSizeExponent) {
        this.gameRunnableEventQueueConsumerService = gameRunnableEventQueueConsumerService;
        this.domainEventEventQueueConsumerService = domainEventEventQueueConsumerService;
        this.clientEventEventQueueConsumerService = clientEventEventQueueConsumerService;
        this.bufferSizeExponent = bufferSizeExponent;
        this.metrics = new ArrayList<>(3);
        this.metrics.add(new QueueMetric());
        this.metrics.add(new QueueMetric());
        this.metrics.add(new QueueMetric());

        this.metrics.get(0).setName(gameRunnableEventQueueConsumerService.getName());
        this.metrics.get(1).setName(domainEventEventQueueConsumerService.getName());
        this.metrics.get(2).setName(clientEventEventQueueConsumerService.getName());
    }

    @PostConstruct
    public void start() {
        // Specify the size of the ring buffer, must be power of 2.
        int bufferSize = (int) Math.pow(2, this.bufferSizeExponent);

        // Construct the Disruptor
        final AtomicInteger threadCount = new AtomicInteger(0);
        this.disruptor = new Disruptor<>(DisruptorEvent::new,
                bufferSize,
                Executors.newCachedThreadPool(runnable -> {
                    Thread thread = Executors
                            .defaultThreadFactory()
                            .newThread(runnable);
                    thread.setName("disruptor-thread-" + threadCount.incrementAndGet());
                    return thread;
                }),
                ProducerType.MULTI,
                new BlockingWaitStrategy());

        // Connect the handler
        EventHandlerGroup<DisruptorEvent> eventHandlerGroup =
                disruptor
                        .handleEventsWith(this::handleGameLoopEventHandler);


        if (this.clientEventEventQueueConsumerService.isEnabled()) {
            eventHandlerGroup.then(this::handleDomainEventHandler, this::handleClientEventHandler);
        } else {
            eventHandlerGroup.then(this::handleDomainEventHandler);
        }

        // Start the Disruptor, starts all threads running
        disruptor.start();

        // Get the ring buffer from the Disruptor to be used for publishing.
        this.ringBuffer = disruptor.getRingBuffer();
    }

    @PreDestroy
    public void destroy() {
        this.ringBuffer = null;
        disruptor.halt();
        disruptor.shutdown();
    }

    private void handleGameLoopEventHandler(DisruptorEvent event, long sequence, boolean endOfBatch) {
        if (event.getGameRunnable() != null) {
            this.metrics.get(0).startClock();
            try {
                if (domainEventEventQueueConsumerService.isEnabled()) {
                    GameEvents.getDomainEvents().setConsumer(event::add);
                } else {
                    GameEvents.getDomainEvents().setConsumer(DOMAIN_EVENT_NOOP);
                }
                if (clientEventEventQueueConsumerService.isEnabled()) {
                    GameEvents.getClientEvents().setConsumer(event::add);
                } else {
                    GameEvents.getClientEvents().setConsumer(CLIENT_EVENT_NOOP);
                }
                this.gameRunnableEventQueueConsumerService.accept(event.getGameRunnable());
            } finally {
                this.metrics.get(0).stopClock();
            }
            event.gameRunnable = null;
        }
    }

    private void handleDomainEventHandler(DisruptorEvent event, long sequence, boolean endOfBatch) {
        if (!event.getDomainEventList().isEmpty()) {
            this.metrics.get(1).startClock();
            try {
                event.getDomainEventList()
                        .forEach(this.domainEventEventQueueConsumerService::accept);
            } finally {
                event.getDomainEventList().clear();
                this.metrics.get(1).stopClock();
            }
        }
    }

    private void handleClientEventHandler(DisruptorEvent event, long sequence, boolean endOfBatch) {
        if (!event.getClientEventList().isEmpty()) {
            this.metrics.get(2).startClock();
            try {
                event.getClientEventList()
                        .forEach(this.clientEventEventQueueConsumerService::accept);
            } finally {
                event.getClientEventList().clear();
                this.metrics.get(2).stopClock();
            }
        }
    }

    void enqueueGame(GameLoopService.GameRunnable gameRunnable) {
        this.ringBuffer.publishEvent(gameTranslator, gameRunnable);
    }

    void enqueueClient(Dto dto) {
        this.ringBuffer.publishEvent(dtoTranslator, dto);
    }

    @Produces
    public GameQueueService getGameQueueService() {
        return this::enqueueGame;
    }

    private void translate(DisruptorEvent event, long sequence, GameLoopService.GameRunnable runnable) {
        event.clear();
        event.setGameRunnable(runnable);
    }

    private void translate(DisruptorEvent event, long sequence, Dto dto) {
        event.clear();
        event.add(dto);
    }

    public static class DisruptorEvent {

        private GameLoopService.GameRunnable gameRunnable;
        private final List<DomainEvent> domainEventList = new ArrayList<>();
        private final List<Dto> clientEventList = new ArrayList<>();

        public GameLoopService.GameRunnable getGameRunnable() {
            return gameRunnable;
        }

        public void setGameRunnable(GameLoopService.GameRunnable gameRunnable) {
            this.gameRunnable = gameRunnable;
        }

        public List<DomainEvent> getDomainEventList() {
            return domainEventList;
        }

        public void add(DomainEvent domainEvent) {
            domainEventList.add(domainEvent);
        }

        public List<Dto> getClientEventList() {
            return clientEventList;
        }

        public void add(Dto clientEvent) {
            clientEventList.add(clientEvent);
        }

        public void clear() {
            this.gameRunnable = null;
            this.domainEventList.clear();
            this.clientEventList.clear();
        }
    }

    @Scheduled(every = QueueMetric.LOAD_FACTOR_CALC_WINDOW_SECONDS + "s")
    public void reportMetrics() {
        this.metrics.forEach(QueueMetric::report);
    }

}
