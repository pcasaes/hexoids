package me.pcasaes.hexoids.infrastructure.disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import io.quarkus.scheduler.Scheduled;
import me.pcasaes.hexoids.domain.eventqueue.GameQueue;
import me.pcasaes.hexoids.domain.model.GameEvents;
import me.pcasaes.hexoids.domain.service.GameLoopService;
import me.pcasaes.hexoids.entrypoints.jobs.periodictasks.FlushClientBroadcasterPeriodicTask;
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

@ApplicationScoped
public class DisruptorIn {

    private final GameLoopService gameLoopService;
    private final int bufferSizeExponent;
    private final List<QueueMetric> metrics;

    private final EventTranslatorOneArg<DisruptorInEvent, Runnable> gameTranslator = this::translate;
    private RingBuffer<DisruptorInEvent> ringBuffer;
    private Disruptor<DisruptorInEvent> disruptor;

    @Inject
    public DisruptorIn(
            GameLoopService gameLoopService,
            @ConfigProperty(
                    name = "hexoids.config.service.disruptor.buffer-size-exponent",
                    defaultValue = "17"
            ) int bufferSizeExponent) {
        this.gameLoopService = gameLoopService;
        this.bufferSizeExponent = bufferSizeExponent;
        this.metrics = new ArrayList<>(1);
        this.metrics.add(new QueueMetric());

        this.metrics.get(0).setName(gameLoopService.getName());
    }

    @PostConstruct
    public void start() {
        // Specify the size of the ring buffer, must be power of 2.
        int bufferSize = (int) Math.pow(2, this.bufferSizeExponent);

        // Construct the Disruptor
        final AtomicInteger threadCount = new AtomicInteger(0);
        this.disruptor = new Disruptor<>(DisruptorInEvent::new,
                bufferSize,
                Executors.newCachedThreadPool(runnable -> {
                    Thread thread = Executors
                            .defaultThreadFactory()
                            .newThread(runnable);
                    thread.setName("disruptor-in-thread-" + threadCount.incrementAndGet());
                    return thread;
                }),
                ProducerType.MULTI,
                new BlockingWaitStrategy());

        // Connect the handler
        disruptor
                .handleEventsWith(this::handleGameLoopEventHandler);


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

    private void handleGameLoopEventHandler(DisruptorInEvent event, long sequence, boolean endOfBatch) {
        if (event.getGameRunnable() != null) {
            this.metrics.get(0).startClock();
            try {
                this.gameLoopService.accept(event.getGameRunnable());
            } finally {
                this.metrics.get(0).stopClock();
            }
            event.gameRunnable = null;
        }
    }

    public void enqueueGame(Runnable gameRunnable) {
        this.ringBuffer.publishEvent(gameTranslator, gameRunnable);
    }

    public void enqueueClient(final Dto dto) {
        this.ringBuffer
                .publishEvent(gameTranslator,
                        () -> GameEvents
                                .getClientEvents()
                                .dispatch(dto)
                );
    }

    @Produces
    public GameQueue getGameQueueService() {
        return this::enqueueGame;
    }

    @Produces
    public FlushClientBroadcasterPeriodicTask.ClientQueue getClientQueue() {
        return this::enqueueClient;
    }

    private void translate(DisruptorInEvent event, long sequence, Runnable runnable) {
        event
                .clear()
                .setGameRunnable(runnable);
    }

    public static class DisruptorInEvent {

        private Runnable gameRunnable;

        public Runnable getGameRunnable() {
            return gameRunnable;
        }

        public DisruptorInEvent setGameRunnable(Runnable gameRunnable) {
            this.gameRunnable = gameRunnable;
            return this;
        }

        public DisruptorInEvent clear() {
            this.gameRunnable = null;
            return this;
        }
    }

    @Scheduled(every = QueueMetric.LOAD_FACTOR_CALC_WINDOW_SECONDS + "s")
    public void reportMetrics() {
        this.metrics.forEach(QueueMetric::report);
    }

}
