package me.pcasaes.hexoids.configuration

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import jakarta.inject.Singleton
import me.pcasaes.hexoids.entrypoints.jobs.periodictasks.FlushClientBroadcasterPeriodicTask.ClientQueue
import me.pcasaes.hexoids.infrastructure.disruptor.DisruptorIn

@ApplicationScoped
class FlushClientBroadcasterPeriodicTaskClientQueueProvider @Inject constructor(
    private val disruptorIn: DisruptorIn
) {

    @Produces
    @Singleton
    fun getClientQueue(): ClientQueue {
        return ClientQueue { dto -> disruptorIn.enqueueClient(dto) }
    }
}
