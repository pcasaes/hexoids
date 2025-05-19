package me.pcasaes.hexoids.core.application.eventhandlers

import me.pcasaes.hexoids.core.domain.model.DomainEvent
import java.util.function.BooleanSupplier

interface ApplicationConsumers {
    fun onJoinGame(domainEvent: DomainEvent)

    fun onPlayerAction(domainEvent: DomainEvent)

    fun onBoltLifeCycle(domainEvent: DomainEvent)

    fun onBoltAction(domainEvent: DomainEvent)

    fun onScoreBoardControl(domainEvent: DomainEvent)

    fun onScoreBoardUpdate(domainEvent: DomainEvent)

    fun interface HaveStarted : BooleanSupplier
}
