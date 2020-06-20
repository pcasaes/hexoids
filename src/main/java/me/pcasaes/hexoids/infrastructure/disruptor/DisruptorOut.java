package me.pcasaes.hexoids.infrastructure.disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import me.pcasaes.hexoids.core.domain.model.DomainEvent;
import me.pcasaes.hexoids.core.domain.model.GameEvents;
import me.pcasaes.hexoids.infrastructure.producer.ClientEventProducer;
import me.pcasaes.hexoids.infrastructure.producer.DomainEventProducer;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pcasaes.hexoids.proto.Dto;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@ApplicationScoped
public class DisruptorOut {
    private static final Consumer<Dto> CLIENT_EVENT_NOOP = v -> {
    };

    private final DomainEventProducer domainEventProducer;
    private final ClientEventProducer clientEventProducer;
    private final int bufferSizeExponent;
    private final List<QueueMetric> metrics;

    private final EventTranslatorOneArg<DisruptorOutEvent, Dto> dtoTranslator = this::translate;
    private final EventTranslatorOneArg<DisruptorOutEvent, DomainEvent> domainEventTranslator = this::translate;

    private RingBuffer<DisruptorOutEvent> ringBuffer;
    private Disruptor<DisruptorOutEvent> disruptor;

    @Inject
    public DisruptorOut(
            DomainEventProducer domainEventProducer,
            ClientEventProducer clientEventProducer,
            @ConfigProperty(
                    name = "hexoids.config.service.disruptor.buffer-size-exponent",
                    defaultValue = "17"
            ) int bufferSizeExponent) {
        this.domainEventProducer = domainEventProducer;
        this.clientEventProducer = clientEventProducer;
        this.bufferSizeExponent = bufferSizeExponent;
        this.metrics = new ArrayList<>(2);
        this.metrics.add(QueueMetric.of(domainEventProducer.getName()));
        this.metrics.add(QueueMetric.of(clientEventProducer.getName()));
    }

    public void startup(@Observes StartupEvent event) {
        // eager load
    }

    @PostConstruct
    public void start() {
        // Specify the size of the ring buffer, must be power of 2.
        int bufferSize = (int) Math.pow(2, this.bufferSizeExponent);

        // Construct the Disruptor
        final AtomicInteger threadCount = new AtomicInteger(0);
        this.disruptor = new Disruptor<>(DisruptorOutEvent::new,
                bufferSize,
                Executors.newCachedThreadPool(runnable -> {
                    Thread thread = Executors
                            .defaultThreadFactory()
                            .newThread(runnable);
                    thread.setName("disruptor-out-thread-" + threadCount.incrementAndGet());
                    return thread;
                }),
                ProducerType.SINGLE,
                new BlockingWaitStrategy());


        wireDomainEventsInterfaces();

        // Start the Disruptor, starts all threads running
        disruptor.start();

        // Get the ring buffer from the Disruptor to be used for publishing.
        this.ringBuffer = disruptor.getRingBuffer();
    }

    /**
     * @see GameEvents#registerEventDispatcher(Consumer)
     */
    private void wireDomainEventsInterfaces() {
        // Connect the handler
        if (this.clientEventProducer.isEnabled()) {
            disruptor.handleEventsWith(this::handleDomainEventHandler, this::handleClientEventHandler);
            GameEvents.getDomainEvents().registerEventDispatcher(this::enqueueDomainEvent);
            GameEvents.getClientEvents().registerEventDispatcher(this::enqueueClient);
        } else {
            disruptor.handleEventsWith(this::handleDomainEventHandler);
            GameEvents.getDomainEvents().registerEventDispatcher(this::enqueueDomainEvent);
            GameEvents.getClientEvents().registerEventDispatcher(CLIENT_EVENT_NOOP);
        }
    }


    @PreDestroy
    public void destroy() {
        this.ringBuffer = null;
        disruptor.halt();
        disruptor.shutdown();
    }

    private void handleDomainEventHandler(DisruptorOutEvent event, long sequence, boolean endOfBatch) {
        DomainEvent domainEvent = event.getDomainEvent();
        if (domainEvent != null) {
            this.metrics.get(0).startClock();
            try {
                this.domainEventProducer.accept(domainEvent);
            } finally {
                event.setDomainEvent(null);
                this.metrics.get(0).stopClock();
            }
        }
    }

    private void handleClientEventHandler(DisruptorOutEvent event, long sequence, boolean endOfBatch) {
        Dto clientEvent = event.getClientEvent();
        if (clientEvent != null) {
            this.metrics.get(1).startClock();
            try {
                this.clientEventProducer.accept(clientEvent);
            } finally {
                event.setClientEvent(null);
                this.metrics.get(1).stopClock();
            }
        }
    }

    void enqueueClient(Dto dto) {
        this.ringBuffer.publishEvent(dtoTranslator, dto);
    }

    void enqueueDomainEvent(DomainEvent domainEvent) {
        this.ringBuffer.publishEvent(domainEventTranslator, domainEvent);
    }

    private void translate(DisruptorOutEvent event, long sequence, DomainEvent domainEvent) {
        event.clear()
                .setDomainEvent(domainEvent);
    }

    private void translate(DisruptorOutEvent event, long sequence, Dto dto) {
        event.clear()
                .setClientEvent(dto);
    }

    public static class DisruptorOutEvent {

        private DomainEvent domainEvent;
        private Dto clientEvent;

        public DomainEvent getDomainEvent() {
            return domainEvent;
        }

        public DisruptorOutEvent setDomainEvent(DomainEvent domainEvent) {
            this.domainEvent = domainEvent;
            return this;
        }

        public Dto getClientEvent() {
            return clientEvent;
        }

        public DisruptorOutEvent setClientEvent(Dto clientEvent) {
            this.clientEvent = clientEvent;
            return this;
        }

        public DisruptorOutEvent clear() {
            this.domainEvent = null;
            this.clientEvent = null;
            return this;
        }
    }

    @Scheduled(every = QueueMetric.LOAD_FACTOR_CALC_WINDOW_SECONDS + "s")
    public void reportMetrics() {
        this.metrics.forEach(QueueMetric::report);
    }


}
