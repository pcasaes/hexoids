package me.pcasaes.hexoids.entrypoints.jobs.periodictasks

import io.quarkus.arc.properties.IfBuildProperty
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.inject.Named
import me.pcasaes.hexoids.core.domain.periodictasks.GamePeriodicTask
import org.eclipse.microprofile.config.inject.ConfigProperty
import pcasaes.hexoids.proto.Dto
import pcasaes.hexoids.proto.Flush
import java.util.function.Consumer

@ApplicationScoped
@IfBuildProperty(name = "hexoids.config.service.client-broadcast.enabled", stringValue = "true", enableIfMissing = true)
@Named("FlushClientBroadcasterPeriodicTask")
class FlushClientBroadcasterPeriodicTask @Inject constructor(
    private val clientQueue: ClientQueue,
    @param:ConfigProperty(
        name = "hexoids.config.service.client-broadcast.batch.timeout",
        defaultValue = "20"
    ) private val batchTimeout: Int
) : GamePeriodicTask {
    private fun flush() {
        clientQueue.accept(FLUSH)
    }

    override fun getDelay(): Long {
        return 1000
    }

    override fun getPeriod(): Long {
        return this.batchTimeout.toLong()
    }

    override fun run() {
        this.flush()
    }

    fun interface ClientQueue : Consumer<Dto>
    companion object {
        private val FLUSH = Dto
            .newBuilder()
            .setFlush(Flush.newBuilder())
            .build()
    }
}
