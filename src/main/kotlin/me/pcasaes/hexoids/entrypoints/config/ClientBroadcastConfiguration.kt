package me.pcasaes.hexoids.entrypoints.config

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault

@ConfigMapping(prefix = "hexoids.service.client-broadcast")
interface ClientBroadcastConfiguration {

    @WithDefault("true")
    fun enabled(): Boolean

    fun batch(): Batch

    interface Batch {

        @WithDefault("64")
        fun size(): Int

        @WithDefault("20")
        fun timeout(): Int
    }

}