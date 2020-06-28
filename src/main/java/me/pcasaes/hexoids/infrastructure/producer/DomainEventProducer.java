package me.pcasaes.hexoids.infrastructure.producer;

import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import me.pcasaes.hexoids.core.domain.model.DomainEvent;
import me.pcasaes.hexoids.core.domain.model.GameTopic;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import pcasaes.hexoids.proto.Event;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Used to generate domain events. Domain events are used to keep server nodes in sync
 */
@ApplicationScoped
public class DomainEventProducer {

    private static final String NAME = "domain-event-producer";

    private final Emitter<Event>[] emitters;

    @Inject
    public DomainEventProducer(
            @Channel("join-game-out") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) Emitter<Event> joinGameEmitter,
            @Channel("player-action-out") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) Emitter<Event> playerActionEmitter,
            @Channel("bolt-life-cycle-out") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) Emitter<Event> boltLifeCycleEmitter,
            @Channel("bolt-action-out") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) Emitter<Event> boltActionEmitter,
            @Channel("score-board-control-out") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) Emitter<Event> scoreBoardControlEmitter,
            @Channel("score-board-update-out") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) Emitter<Event> scoreBoardUpdateEmitter
    ) {
        Emitter<Event>[] em = new Emitter[GameTopic.values().length];
        em[GameTopic.JOIN_GAME_TOPIC.ordinal()] = joinGameEmitter;
        em[GameTopic.PLAYER_ACTION_TOPIC.ordinal()] = playerActionEmitter;
        em[GameTopic.BOLT_LIFECYCLE_TOPIC.ordinal()] = boltLifeCycleEmitter;
        em[GameTopic.BOLT_ACTION_TOPIC.ordinal()] = boltActionEmitter;
        em[GameTopic.SCORE_BOARD_CONTROL_TOPIC.ordinal()] = scoreBoardControlEmitter;
        em[GameTopic.SCORE_BOARD_UPDATE_TOPIC.ordinal()] = scoreBoardUpdateEmitter;

        this.emitters = em;
    }

    public void accept(DomainEvent event) {
        if (event != null) {
            this.emitters[GameTopic.valueOf(event.getTopic()).ordinal()]
                    .send(KafkaRecord.of(event.getKey(), event.getEvent()));
        }
    }

    public String getName() {
        return NAME;
    }
}
