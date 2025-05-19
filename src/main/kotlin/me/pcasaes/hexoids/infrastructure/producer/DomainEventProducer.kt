package me.pcasaes.hexoids.infrastructure.producer

import io.smallrye.reactive.messaging.kafka.Record
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import me.pcasaes.hexoids.core.domain.model.DomainEvent
import me.pcasaes.hexoids.core.domain.model.GameTopic
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.OnOverflow
import pcasaes.hexoids.proto.Event
import java.util.UUID

/**
 * Used to generate domain events. Domain events are used to keep server nodes in sync
 */
@ApplicationScoped
class DomainEventProducer @Inject constructor(
    @Channel("join-game-out") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) joinGameEmitter: Emitter<Record<UUID, Event?>>,
    @Channel("player-action-out") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) playerActionEmitter: Emitter<Record<UUID, Event?>>,
    @Channel("bolt-life-cycle-out") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) boltLifeCycleEmitter: Emitter<Record<UUID, Event?>>,
    @Channel("bolt-action-out") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) boltActionEmitter: Emitter<Record<UUID, Event?>>,
    @Channel("score-board-control-out") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) scoreBoardControlEmitter: Emitter<Record<UUID, Event?>>,
    @Channel("score-board-update-out") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) scoreBoardUpdateEmitter: Emitter<Record<UUID, Event?>>
) {
    private val emitters: Array<Emitter<Record<UUID, Event?>>?>

    init {
        val em = arrayOfNulls<Emitter<Record<UUID, Event?>>?>(GameTopic.entries.size)
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
                ?.send(Record.of(event.key, event.event))
        }
    }
}
