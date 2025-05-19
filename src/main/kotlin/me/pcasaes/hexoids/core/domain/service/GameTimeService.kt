package me.pcasaes.hexoids.core.domain.service

import me.pcasaes.hexoids.core.domain.model.Game.Companion.get

class GameTimeService private constructor() {
    fun getTime(): Long {
        return get().getClock().getTime()
    }

    companion object {
        private val INSTANCE = GameTimeService()

        fun getInstance(): GameTimeService {
            return INSTANCE
        }
    }
}
