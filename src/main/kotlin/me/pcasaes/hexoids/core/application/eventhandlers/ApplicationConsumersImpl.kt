package me.pcasaes.hexoids.core.application.eventhandlers

import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue
import me.pcasaes.hexoids.core.domain.model.DomainEvent
import me.pcasaes.hexoids.core.domain.model.GameTopic
import java.util.logging.Level
import java.util.logging.Logger

class ApplicationConsumersImpl private constructor(private val gameQueue: GameQueue) : ApplicationConsumers {
    companion object {
        private val LOGGER: Logger = Logger.getLogger(ApplicationConsumersImpl::class.java.getName())

        fun create(gameQueue: GameQueue): ApplicationConsumers {
            return ApplicationConsumersImpl(gameQueue)
        }
    }

    private fun process(domainEvent: DomainEvent, consumer: GameTopic) {
        try {
            gameQueue.enqueue(Runnable { consumer.consume(domainEvent) })
        } catch (ex: RuntimeException) {
            LOGGER.log(Level.SEVERE, ex.message, ex)
        }
    }

    override fun onJoinGame(domainEvent: DomainEvent) {
        process(domainEvent, GameTopic.JOIN_GAME_TOPIC)
    }

    override fun onPlayerAction(domainEvent: DomainEvent) {
        process(domainEvent, GameTopic.PLAYER_ACTION_TOPIC)
    }

    override fun onBoltLifeCycle(domainEvent: DomainEvent) {
        process(domainEvent, GameTopic.BOLT_LIFECYCLE_TOPIC)
    }

    override fun onBoltAction(domainEvent: DomainEvent) {
        process(domainEvent, GameTopic.BOLT_ACTION_TOPIC)
    }

    override fun onScoreBoardControl(domainEvent: DomainEvent) {
        process(domainEvent, GameTopic.SCORE_BOARD_CONTROL_TOPIC)
    }

    override fun onScoreBoardUpdate(domainEvent: DomainEvent) {
        process(domainEvent, GameTopic.SCORE_BOARD_UPDATE_TOPIC)
    }


}
