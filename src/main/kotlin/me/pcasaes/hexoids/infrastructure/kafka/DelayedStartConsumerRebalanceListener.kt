package me.pcasaes.hexoids.infrastructure.kafka

import io.smallrye.reactive.messaging.kafka.KafkaConsumerRebalanceListener
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.inject.Named
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.TopicPartition

@ApplicationScoped
@Named("DelayedStartConsumerRebalanceListener")
class DelayedStartConsumerRebalanceListener @Inject constructor(
    private val handler: DelayedStartConsumerHandler
) : KafkaConsumerRebalanceListener {

    override fun onPartitionsAssigned(consumer: Consumer<*, *>, partitions: MutableCollection<TopicPartition>) {
        this.handler
            .onStarted()
            .await()
            .indefinitely()
    }
}
