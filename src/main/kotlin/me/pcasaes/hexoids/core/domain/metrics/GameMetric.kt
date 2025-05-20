package me.pcasaes.hexoids.core.domain.metrics

import pcasaes.hexoids.proto.ClientPlatforms

class GameMetric private constructor(
    private val name: String
) {


    companion object {

        fun of(name: String): GameMetric {
            return GameMetric(name)
        }
    }

    private var total = 0L

    private val totalByClientPlatform: LongArray = ClientPlatforms
        .entries
        .map { 0L }
        .toLongArray()

    fun increment(clientPlatform: ClientPlatforms?) {
        total++
        if (clientPlatform == null) {
            totalByClientPlatform[ClientPlatforms.UNKNOWN.ordinal]++
        } else {
            totalByClientPlatform[clientPlatform.ordinal]++
        }
    }

    fun getTotal(): Long {
        return total
    }

    fun getTotalByClientPlatform(clientPlatform: ClientPlatforms): Long {
        return totalByClientPlatform[clientPlatform.ordinal]
    }

    fun getName(): String {
        return name
    }

}
