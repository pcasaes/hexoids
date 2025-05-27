package me.pcasaes.hexoids.infrastructure.producer

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import me.pcasaes.hexoids.core.domain.model.DomainEvent
import me.pcasaes.hexoids.core.domain.model.GameTopic
import me.pcasaes.hexoids.core.domain.model.EventRecord
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.OnOverflow

/**
 * Used to generate domain events. Domain events are used to keep server nodes in sync
 */
@ApplicationScoped
class DomainEventProducer @Inject constructor(
    @Channel("join-game-out") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) joinGameEmitter: Emitter<EventRecord>,
    @Channel("player-action-out") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) playerActionEmitter: Emitter<EventRecord>,
    @Channel("bolt-life-cycle-out") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) boltLifeCycleEmitter: Emitter<EventRecord>,
    @Channel("bolt-action-out") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) boltActionEmitter: Emitter<EventRecord>,
    @Channel("score-board-control-out") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) scoreBoardControlEmitter: Emitter<EventRecord>,
    @Channel("score-board-update-out") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) scoreBoardUpdateEmitter: Emitter<EventRecord>
) {
    private val emitters: Array<Emitter<EventRecord>?>

    init {
        val em = arrayOfNulls<Emitter<EventRecord>?>(GameTopic.entries.size)
        em[GameTopic.JOIN_GAME_TOPIC.ordinal] = joinGameEmitter
        em[GameTopic.PLAYER_ACTION_TOPIC.ordinal] = playerActionEmitter
        em[GameTopic.BOLT_LIFECYCLE_TOPIC.ordinal] = boltLifeCycleEmitter
        em[GameTopic.BOLT_ACTION_TOPIC.ordinal] = boltActionEmitter
        em[GameTopic.SCORE_BOARD_CONTROL_TOPIC.ordinal] = scoreBoardControlEmitter
        em[GameTopic.SCORE_BOARD_UPDATE_TOPIC.ordinal] = scoreBoardUpdateEmitter

        this.emitters = em
    }

    fun accept(event: DomainEvent?) {
        val topic = event?.topic
        if (topic != null) {
            this.emitters[GameTopic.valueOf(topic).ordinal]
                ?.send(EventRecord.of(event))
        }
    }
}
