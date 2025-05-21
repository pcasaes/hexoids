package me.pcasaes.hexoids.infrastructure.kafka

import io.smallrye.reactive.messaging.kafka.KafkaConsumerRebalanceListener
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.inject.Named
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.TopicPartition
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.logging.Logger

@ApplicationScoped
@Named("hexoids-server-bolt-life-cycle")
class KafkaBoltLifeCycleConsumerRebalanceListener @Inject constructor(
    private val handler: DelayedStartConsumerHandler,
    @param:ConfigProperty(
        name = "hexoids.config.bolt.max.duration",
        defaultValue = "10000"
    ) private val boltMaxDuration: Int
) : KafkaConsumerRebalanceListener {

    override fun onPartitionsAssigned(consumer: Consumer<*, *>, partitions: MutableCollection<TopicPartition>) {
        val now = System.currentTimeMillis()
        val shouldStartAt = now - (this.boltMaxDuration + 10_000L)

        consumer.offsetsForTimes(
            partitions.associateWith { shouldStartAt }
        ).forEach { (k, v) ->
            if (v != null) {
                LOGGER.info("Seeking to $v")
                consumer.seek(k, v.offset())
                LOGGER.info("Seeked to $v")
            }
        }

        handler
            .onStarted()
            .await()
            .indefinitely()
    }


    companion object {
        private val LOGGER: Logger = Logger.getLogger(KafkaBoltLifeCycleConsumerRebalanceListener::class.java.getName())
    }
}
