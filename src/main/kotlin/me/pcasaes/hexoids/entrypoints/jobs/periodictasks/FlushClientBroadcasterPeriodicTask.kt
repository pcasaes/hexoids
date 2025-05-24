package me.pcasaes.hexoids.entrypoints.jobs.periodictasks

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.inject.Named
import me.pcasaes.hexoids.core.domain.periodictasks.GamePeriodicTask
import me.pcasaes.hexoids.entrypoints.config.ClientBroadcastConfiguration
import pcasaes.hexoids.proto.Dto
import pcasaes.hexoids.proto.Flush
import java.util.function.Consumer

@ApplicationScoped
@Named("FlushClientBroadcasterPeriodicTask")
class FlushClientBroadcasterPeriodicTask @Inject constructor(
    private val clientQueue: ClientQueue,
    private val clientBroadcastConfiguration: ClientBroadcastConfiguration,
) : GamePeriodicTask {

    override fun enabled(): Boolean {
        return clientBroadcastConfiguration.enabled()
    }

    private fun flush() {
        clientQueue.accept(FLUSH)
    }

    override fun getDelay(): Long {
        return 1000
    }

    override fun getPeriod(): Long {
        return clientBroadcastConfiguration.batch().timeout().toLong()
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
