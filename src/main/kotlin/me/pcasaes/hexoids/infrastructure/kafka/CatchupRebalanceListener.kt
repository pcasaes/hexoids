package me.pcasaes.hexoids.infrastructure.kafka

import io.smallrye.reactive.messaging.kafka.KafkaConsumerRebalanceListener
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.inject.Named
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.TopicPartition
import java.time.Duration

@ApplicationScoped
@Named("CatchupRebalanceListener")
class CatchupRebalanceListener @Inject constructor(
    private val delayedStartConsumerHandler: DelayedStartConsumerHandler
) : KafkaConsumerRebalanceListener {

    override fun onPartitionsAssigned(consumer: Consumer<*, *>, partitions: MutableCollection<TopicPartition>) {
        delayedStartConsumerHandler.register(consumer.endOffsets(partitions, Duration.ofSeconds(30)))
    }
}
