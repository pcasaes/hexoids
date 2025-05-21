package me.pcasaes.hexoids.core.domain.service

import me.pcasaes.hexoids.core.domain.model.Game.Companion.get

object GameTimeService {
    fun getTime(): Long {
        return get().getClock().getTime()
    }
}
