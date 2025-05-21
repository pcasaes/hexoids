package me.pcasaes.hexoids.infrastructure.disruptor

import com.lmax.disruptor.BlockingWaitStrategy
import com.lmax.disruptor.EventHandler
import com.lmax.disruptor.EventTranslatorOneArg
import com.lmax.disruptor.RingBuffer
import com.lmax.disruptor.dsl.Disruptor
import com.lmax.disruptor.dsl.ProducerType
import io.quarkus.runtime.StartupEvent
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import me.pcasaes.hexoids.core.domain.model.DomainEvent
import me.pcasaes.hexoids.core.domain.model.GameEvents
import me.pcasaes.hexoids.core.domain.model.GameEvents.Companion.getClientEvents
import me.pcasaes.hexoids.core.domain.model.GameEvents.Companion.getDomainEvents
import me.pcasaes.hexoids.infrastructure.clock.HRClock.nanoTime
import me.pcasaes.hexoids.infrastructure.producer.ClientEventProducer
import me.pcasaes.hexoids.infrastructure.producer.DomainEventProducer
import org.eclipse.microprofile.config.inject.ConfigProperty
import pcasaes.hexoids.proto.Dto
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import kotlin.math.pow

@ApplicationScoped
class DisruptorOut @Inject constructor(
    private val domainEventProducer: DomainEventProducer,
    private val clientEventProducer: ClientEventProducer,
    @param:ConfigProperty(
        name = "hexoids.config.service.disruptor.buffer-size-exponent",
        defaultValue = "17"
    ) private val bufferSizeExponent: Int
) {
    private val metrics: MutableList<QueueMetric>

    private val dtoTranslator = EventTranslatorOneArg { event: DisruptorOutEvent, _, dto: Dto ->
        this.translate(
            event, dto
        )
    }
    private val domainEventTranslator =
        EventTranslatorOneArg { event: DisruptorOutEvent, _, domainEvent: DomainEvent ->
            this.translate(
                event, domainEvent
            )
        }

    private lateinit var ringBuffer: RingBuffer<DisruptorOutEvent>
    private lateinit var disruptor: Disruptor<DisruptorOutEvent>

    init {
        val m = ArrayList<QueueMetric>(2)
        m.add(QueueMetric.of(METRIC_DOMAIN_EVENT_OUT))
        m.add(QueueMetric.of(METRIC_CLIENT_EVENT_OUT))
        this.metrics = m
    }

    fun startup(@Observes event: StartupEvent) {
        // eager load
    }

    @PostConstruct
    fun start() {
        // Specify the size of the ring buffer, must be power of 2.
        val bufferSize = 2.0.pow(this.bufferSizeExponent.toDouble()).toInt()

        // Construct the Disruptor
        val threadCount = AtomicInteger(0)
        this.disruptor = Disruptor<DisruptorOutEvent>(
            { DisruptorOutEvent() },
            bufferSize,
            { runnable ->
                val thread = Executors
                    .defaultThreadFactory()
                    .newThread(runnable)
                thread.setName("disruptor-out-thread-" + threadCount.incrementAndGet())
                thread
            },
            ProducerType.SINGLE,
            BlockingWaitStrategy()
        )


        wireDomainEventsInterfaces()

        // Start the Disruptor, starts all threads running
        disruptor.start()

        // Get the ring buffer from the Disruptor to be used for publishing.
        this.ringBuffer = disruptor.getRingBuffer()
    }

    /**
     * @see GameEvents.registerEventDispatcher
     */
    private fun wireDomainEventsInterfaces() {
        // Connect the handler
        if (this.clientEventProducer.isEnabled()) {
            disruptor.handleEventsWith(EventHandler { event: DisruptorOutEvent, _, _ ->
                this.handleDomainEventHandler(
                    event
                )
            }, EventHandler { event: DisruptorOutEvent, _, _ ->
                this.handleClientEventHandler(
                    event
                )
            })
            getDomainEvents().registerEventDispatcher { domainEvent ->
                this.enqueueDomainEvent(
                    domainEvent
                )
            }
            getClientEvents().registerEventDispatcher { dto -> this.enqueueClient(dto) }
        } else {
            disruptor.handleEventsWith(EventHandler { event: DisruptorOutEvent, _, _ ->
                this.handleDomainEventHandler(
                    event
                )
            })
            getDomainEvents().registerEventDispatcher { domainEvent ->
                this.enqueueDomainEvent(
                    domainEvent
                )
            }
            getClientEvents().registerEventDispatcher(CLIENT_EVENT_NOOP)
        }
    }


    @PreDestroy
    fun destroy() {
        disruptor.halt()
        disruptor.shutdown()
    }

    private fun handleDomainEventHandler(event: DisruptorOutEvent) {
        val domainEvent = event.domainEvent
        if (domainEvent != null) {
            val queueMetric = this.metrics[0]
            queueMetric.startClock()
            try {
                this.domainEventProducer.accept(domainEvent)
            } finally {
                queueMetric.stopClock(event.createTime)
                event.domainEvent = null
            }
        }
    }

    private fun handleClientEventHandler(event: DisruptorOutEvent) {
        val clientEvent = event.clientEvent
        if (clientEvent != null) {
            val queueMetric = this.metrics[1]
            queueMetric.startClock()
            try {
                this.clientEventProducer.accept(clientEvent)
            } finally {
                queueMetric.stopClock(event.createTime)
                event.clientEvent = null
            }
        }
    }

    fun enqueueClient(dto: Dto) {
        this.ringBuffer.publishEvent(dtoTranslator, dto)
    }

    fun enqueueDomainEvent(domainEvent: DomainEvent) {
        this.ringBuffer.publishEvent(domainEventTranslator, domainEvent)
    }

    private fun translate(event: DisruptorOutEvent, domainEvent: DomainEvent) {
        event.clear()
            .domainEvent = domainEvent
    }

    private fun translate(event: DisruptorOutEvent, dto: Dto) {
        event.clear()
            .clientEvent = dto
    }

    class DisruptorOutEvent {
        var domainEvent: DomainEvent? = null
            set(domainEvent) {
                markCreateTime()
                field = domainEvent
            }

        var clientEvent: Dto? = null
            set(clientEvent) {
                markCreateTime()
                field = clientEvent
            }

        var createTime: Long = 0
            private set

        fun clear(): DisruptorOutEvent {
            this.domainEvent = null
            this.clientEvent = null
            return this
        }

        private fun markCreateTime() {
            this.createTime = nanoTime()
        }
    }

    companion object {
        const val METRIC_DOMAIN_EVENT_OUT: String = "domain-event-out"
        const val METRIC_CLIENT_EVENT_OUT: String = "client-event-out"


        private val CLIENT_EVENT_NOOP = Consumer { _: Dto -> }
    }
}
