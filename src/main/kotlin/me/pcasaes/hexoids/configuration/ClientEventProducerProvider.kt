package me.pcasaes.hexoids.configuration

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import jakarta.inject.Singleton
import me.pcasaes.hexoids.entrypoints.web.ClientBroadcaster
import me.pcasaes.hexoids.infrastructure.producer.ClientEventProducer
import pcasaes.hexoids.proto.Dto

@ApplicationScoped
class ClientEventProducerProvider @Inject constructor(
    private val clientBroadcaster: ClientBroadcaster
) {
    @Produces
    @Singleton
    fun getClientBroadcaster(): ClientEventProducer {
        return object : ClientEventProducer {
            override fun isEnabled(): Boolean {
                return clientBroadcaster.isEnabled()
            }

            override fun accept(dto: Dto?) {
                clientBroadcaster.accept(dto)
            }
        }
    }
}
