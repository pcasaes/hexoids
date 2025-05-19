package me.pcasaes.hexoids.core.domain.model

import java.util.Random

/**
 * Whenever a player is spawned the class provides the player's X,Y position.
 *
 */
interface ResetPosition {

    fun getNextX(): Float

    fun getNextY(): Float

    object Holder {
        var instance: ResetPosition? = null
    }

    /**
     * [ResetPosition] implementaiton that spawns players in a random
     * location.
     */
    private class RngResetPosition() : ResetPosition {
        override fun getNextX(): Float {
            return RNG.nextFloat()
        }

        override fun getNextY(): Float {
            return RNG.nextFloat()
        }

        companion object {
            private val RNG = Random()
        }
    }

    /**
     * [ResetPosition] implementation that spawns all player in the
     * same X,Y position.
     */
    private class FixedResetPosition(config: String?) : ResetPosition {
        private val x: Float

        private val y: Float

        /**
         * The fixed position is defined in a comma separated list.
         * ex: 0,0
         *
         * Valid values are between 0 and 1.
         *
         * @param config comma separated X,Y position.
         */
        init {
            if (config.isNullOrEmpty()) {
                this.x = 0f
                this.y = 0f
            } else {
                val parts: Array<String> = config.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                this.x = parts[0].toFloat()
                if (parts.size == 1) {
                    this.y = parts[0].toFloat()
                } else {
                    this.y = parts[1].toFloat()
                }
            }
        }

        override fun getNextX(): Float {
            return x
        }

        override fun getNextY(): Float {
            return y
        }
    }

    companion object {
        /**
         * Return a ResetPosition.
         *
         * If config is rng will return [RngResetPosition]
         * Otherwise presumes a comma separated float list and return [FixedResetPosition]
         *
         * @param config used to configure an appropriate implementation
         * @return a ResetPosition
         */
        fun create(config: String): ResetPosition {
            var instance: ResetPosition? = Holder.instance
            if (instance == null) {
                instance = if ("rng".equals(config, ignoreCase = true)) {
                    RngResetPosition()
                } else {
                    FixedResetPosition(config)
                }
                Holder.instance = instance
            }
            return instance
        }
    }
}
