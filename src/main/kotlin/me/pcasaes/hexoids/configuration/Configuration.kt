package me.pcasaes.hexoids.configuration

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import io.smallrye.config.WithName

@ConfigMapping(prefix = "hexoids.config")
interface Configuration {


    @WithDefault("50")
    fun updateFrequencyInMillis(): Long

    @WithName("min.move")
    @WithDefault("0.0001")
    fun minMove(): Float

    fun player(): Player

    fun bolt(): Bolt

    fun blackhole(): Blackhole

    fun inertia(): Inertia

    interface Inertia {
        @WithDefault("-0.001")
        fun dampenCoefficient(): Float
    }

    interface Player {

        @WithName("max.move")
        @WithDefault("10")
        fun maxMove(): Float

        @WithName("max.angle.divisor")
        @WithDefault("4")
        fun maxAngleDivisor(): Float

        @WithDefault("60000")
        fun expungeSinceLastSpawnTimeout(): Long

        @WithName("reset.position")
        @WithDefault("rng")
        fun resetPosition(): String

        @WithName("max.bolts")
        @WithDefault("10")
        fun maxBolts(): Int

        @WithDefault("7")
        fun nameLength(): Int

        fun destroyed(): Destroyed

        interface Destroyed {

            @WithName("shockwave.distance")
            @WithDefault("0.0408")
            fun shockwaveDistance(): Float

            @WithName("shockwave.duration.ms")
            @WithDefault("400")
            fun shockwaveDurationMs(): Long

            @WithName("shockwave.impulse")
            @WithDefault("0.007")
            fun shockwaveImpulse(): Float
        }
    }

    interface Bolt {

        @WithName("max.duration")
        @WithDefault("10000")
        fun maxDuration(): Int

        @WithDefault("0.07")
        fun speed(): Float

        @WithName("collision.radius")
        @WithDefault("0.001")
        fun collisionRadius(): Float

        @WithName("inertia.enabled")
        @WithDefault("true")
        fun inertiaEnabled(): Boolean

        @WithName("inertia.rejection-scale")
        @WithDefault("0.8")
        fun inertiaRejectionScale(): Float

        @WithName("inertia.projection-scale")
        @WithDefault("0.8")
        fun inertiaProjectionScale(): Float

        @WithName("inertia.negative-projection-scale")
        @WithDefault("0.1")
        fun inertiaNegativeProjectionScale(): Float
    }

    interface Blackhole {

        @WithName("eventhorizon.radius")
        @WithDefault("0.005")
        fun eventHorizonRadius(): Float

        @WithName("gravity.radius")
        @WithDefault("0.07")
        fun gravityRadius(): Float

        @WithName("gravity.impulse")
        @WithDefault("0.07")
        fun gravityImpulse(): Float

        @WithName("dampen.factor")
        @WithDefault("5")
        fun dampenFactor(): Float

        @WithName("genesis.probability.factor")
        @WithDefault("3")
        fun genesisProbabilityFactor(): Int

        @WithName("teleport.probability")
        @WithDefault("0.05")
        fun teleportProbability(): Float
    }

}