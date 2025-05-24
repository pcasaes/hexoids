package me.pcasaes.hexoids.infrastructure.disruptor

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault

@ConfigMapping(prefix = "hexoids.service.disruptor")
interface DisruptorConfig {

    @WithDefault("17")
    fun bufferSizeExponent(): Int

}