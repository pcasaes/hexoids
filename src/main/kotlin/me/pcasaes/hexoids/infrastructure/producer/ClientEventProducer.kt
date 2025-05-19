package me.pcasaes.hexoids.infrastructure.producer

import pcasaes.hexoids.proto.Dto

/**
 * Produces events for clients
 */
interface ClientEventProducer {
    fun isEnabled(): Boolean

    fun accept(dto: Dto?)
}
