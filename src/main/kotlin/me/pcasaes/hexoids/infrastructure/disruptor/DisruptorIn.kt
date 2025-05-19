package me.pcasaes.hexoids.infrastructure.disruptor

import com.lmax.disruptor.BlockingWaitStrategy
import com.lmax.disruptor.EventHandler
import com.lmax.disruptor.EventTranslatorOneArg
import com.lmax.disruptor.RingBuffer
import com.lmax.disruptor.dsl.Disruptor
import com.lmax.disruptor.dsl.ProducerType
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue
import me.pcasaes.hexoids.core.domain.model.GameEvents.Companion.getClientEvents
import me.pcasaes.hexoids.core.domain.service.GameLoopService
import me.pcasaes.hexoids.infrastructure.clock.HRClock.nanoTime
import org.eclipse.microprofile.config.inject.ConfigProperty
import pcasaes.hexoids.proto.Dto
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.pow

@ApplicationScoped
class DisruptorIn @Inject constructor(
    private val gameLoopService: GameLoopService,
    @param:ConfigProperty(
        name = "hexoids.config.service.disruptor.buffer-size-exponent",
        defaultValue = "17"
    ) private val bufferSizeExponent: Int
) {
    private val metrics: MutableList<QueueMetric>

    private val gameTranslator =
        EventTranslatorOneArg { event: DisruptorInEvent, _, runnable: Runnable ->
            this.translate(
                event, runnable
            )
        }
    private lateinit var ringBuffer: RingBuffer<DisruptorInEvent>
    private lateinit var disruptor: Disruptor<DisruptorInEvent>

    init {
        val m = ArrayList<QueueMetric>(1)
        m.add(QueueMetric.of(METRIC_GAME_LOOP_IN))
        this.metrics = m
    }

    @PostConstruct
    fun start() {
        // Specify the size of the ring buffer, must be power of 2.
        val bufferSize = 2.0.pow(this.bufferSizeExponent.toDouble()).toInt()

        // Construct the Disruptor
        val threadCount = AtomicInteger(0)
        this.disruptor = Disruptor<DisruptorInEvent>(
            { DisruptorInEvent() },
            bufferSize,
            { runnable ->
                val thread = Executors
                    .defaultThreadFactory()
                    .newThread(runnable)
                thread.setName("disruptor-in-thread-" + threadCount.incrementAndGet())
                thread
            },
            ProducerType.MULTI,
            BlockingWaitStrategy()
        )

        // Connect the handler
        disruptor
            .handleEventsWith(EventHandler { event, _, _ ->
                this.handleGameLoopEventHandler(
                    event
                )
            })


        // Start the Disruptor, starts all threads running
        disruptor.start()

        // Get the ring buffer from the Disruptor to be used for publishing.
        this.ringBuffer = disruptor.getRingBuffer()
    }

    @PreDestroy
    fun destroy() {
        disruptor.halt()
        disruptor.shutdown()
    }

    private fun handleGameLoopEventHandler(event: DisruptorInEvent) {
        val gameRunnable = event.gameRunnable
        if (gameRunnable != null) {
            val queueMetric = this.metrics[0]
            queueMetric.startClock()
            try {
                this.gameLoopService.accept(gameRunnable)
            } finally {
                queueMetric.stopClock(event.createTime)
            }
            event.clear()
        }
    }

    fun enqueueGame(gameRunnable: Runnable) {
        this.ringBuffer.publishEvent(gameTranslator, gameRunnable)
    }

    fun enqueueClient(dto: Dto) {
        this.ringBuffer
            .publishEvent(
                gameTranslator,
                Runnable {
                    getClientEvents()
                        .dispatch(dto)
                }
            )
    }

    @Produces
    fun getGameQueueService(): GameQueue {
        return GameQueue { gameRunnable: Runnable -> this.enqueueGame(gameRunnable) }
    }

    private fun translate(event: DisruptorInEvent, runnable: Runnable?) {
        event
            .clear()
            .gameRunnable = runnable
    }

    class DisruptorInEvent {
        var gameRunnable: Runnable? = null
            set(gameRunnable) {
                this.createTime = nanoTime()
                field = gameRunnable
            }

        var createTime: Long = 0
            private set


        fun clear(): DisruptorInEvent {
            this.gameRunnable = null
            return this
        }
    }

    companion object {
        const val METRIC_GAME_LOOP_IN: String = "game-loop-in"
    }
}
