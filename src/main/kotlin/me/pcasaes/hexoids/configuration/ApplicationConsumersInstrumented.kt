package me.pcasaes.hexoids.configuration

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import me.pcasaes.hexoids.configuration.metrics.ApplicationConsumerMetrics
import me.pcasaes.hexoids.core.application.eventhandlers.ApplicationConsumers
import me.pcasaes.hexoids.core.application.eventhandlers.ApplicationConsumersImpl
import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue

@ApplicationScoped
@ApplicationConsumerMetrics
class ApplicationConsumersInstrumented @Inject constructor(
    gameQueue: GameQueue
) : ApplicationConsumers by ApplicationConsumersImpl.create(gameQueue)