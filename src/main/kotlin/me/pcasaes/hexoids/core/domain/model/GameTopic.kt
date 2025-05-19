package me.pcasaes.hexoids.core.domain.model

/**
 * Enum of game topics use to publish and consume domain events.
 */
enum class GameTopic(private val consumer: (DomainEvent) -> Unit) {
    /**
     * Topic used to track joined and left game
     */
    JOIN_GAME_TOPIC({ d ->
        GameTopic.Companion.getGame()
            .getPlayers().consumeFromJoinTopic(d)
    }),

    /**
     * Topic used to track player actions: moved, spawned, destroyed
     */
    PLAYER_ACTION_TOPIC({ d ->
        GameTopic.Companion.getGame()
            .getPlayers().consumeFromPlayerActionTopic(d)
    }),

    /**
     * Topic used to track a bolts being fired (created)
     */
    BOLT_LIFECYCLE_TOPIC({ d ->
        GameTopic.Companion.getGame()
            .getPlayers().consumeFromPlayerFiredTopic(d)
    }),

    /**
     * Topic used to track bolt action: moved, exhausted
     */
    BOLT_ACTION_TOPIC({ d: DomainEvent ->
        GameTopic.Companion.getGame().getBolts().consumeFromBoltActionTopic(d)
        GameTopic.Companion.getGame()
            .getPlayers().consumeFromBoltActionTopic(d)
    }),

    /**
     * Topic used to track points being gained by a player
     */
    SCORE_BOARD_CONTROL_TOPIC({ d ->
        GameTopic.Companion.getGame()
            .getScoreBoard().consumeFromScoreBoardControlTopic(d)
    }),

    /**
     * Topic used to track points score board being updated
     */
    SCORE_BOARD_UPDATE_TOPIC({ d ->
        GameTopic.Companion.getGame()
            .getScoreBoard().consumeFromScoreBoardUpdateTopic(d)
    }),
    ;


    fun consume(domainEvent: DomainEvent) {
        consumer.invoke(domainEvent)
    }

    companion object {
        private lateinit var game: Game

        @JvmStatic
        fun setGame(game: Game) {
            GameTopic.Companion.game = game
        }

        fun getGame(): Game {
            return game
        }
    }
}
