package me.paulo.casaes.bbop.model;

import me.paulo.casaes.bbop.dto.BoltExhaustedEventDto;
import me.paulo.casaes.bbop.dto.BoltFiredEventDto;
import me.paulo.casaes.bbop.dto.DirectedCommandDto;
import me.paulo.casaes.bbop.dto.EventType;
import me.paulo.casaes.bbop.dto.PlayerDestroyedEventDto;
import me.paulo.casaes.bbop.dto.PlayerJoinedEventDto;
import me.paulo.casaes.bbop.dto.PlayerMovedOrSpawnedEventDto;
import me.paulo.casaes.bbop.dto.PlayersListCommandDto;
import me.paulo.casaes.bbop.model.annotations.IsThreadSafe;
import me.paulo.casaes.bbop.util.concurrent.SingleMutatorMultipleAccessorConcurrentHashMap;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Players implements Iterable<Player> {

    static Players create(Bolts bolts, Clock clock, ScoreBoard scoreBoard) {
        return new Players(bolts, clock, scoreBoard);
    }

    private final Map<UUID, Player> playerMap = new SingleMutatorMultipleAccessorConcurrentHashMap<>(5000, 0.5f);

    private final Bolts bolts;

    private final Clock clock;

    private final ScoreBoard scoreBoard;

    private Players(Bolts bolts, Clock clock, ScoreBoard scoreBoard) {
        this.bolts = bolts;
        this.clock = clock;
        this.scoreBoard = scoreBoard;
    }

    /**
     * If the player hasn't been created will do so and return the player
     *
     * @param id
     * @return
     */
    public Optional<Player> createPlayer(UUID id) {
        if (playerMap.containsKey(id)) {
            return Optional.empty();
        }
        return Optional.of(createOrGet(id));
    }

    public Optional<Player> createPlayer(String id) {
        return createPlayer(UUID.fromString(id));
    }

    public Player createOrGet(UUID id) {
        return playerMap.computeIfAbsent(id, this::create);
    }

    public Player createOrGet(String id) {
        return createOrGet(UUID.fromString(id));
    }

    @IsThreadSafe
    public Optional<Player> get(UUID id) {
        return Optional.ofNullable(playerMap.get(id));
    }

    @IsThreadSafe
    public Optional<Player> get(String id) {
        return get(UUID.fromString(id));
    }

    private Player create(UUID id) {
        requestListOfPlayers(id.toString());
        return Player.create(id, this, this.bolts, this.clock, this.scoreBoard);
    }

    @IsThreadSafe
    public void requestListOfPlayers(String requesterId) {
        GameEvents.getClientEvents().register(
                DirectedCommandDto.of(
                        requesterId,
                        PlayersListCommandDto.of(
                                playerMap
                                        .values()
                                        .stream()
                                        .map(Player::toDto)
                                        .collect(Collectors.toList())
                        )
                )
        );
    }

    @Override
    @IsThreadSafe
    public Iterator<Player> iterator() {
        return playerMap
                .values()
                .iterator();
    }

    public Stream<Player> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    void joined(PlayerJoinedEventDto event) {
        Player player = createOrGet(event.getPlayerId());
        player.joined(event);
    }

    void left(UUID playerId) {
        Player player = createOrGet(playerId);
        playerMap.remove(playerId);
        player.left();
    }

    public void consumeFromJoinTopic(DomainEvent domainEvent) {
        if (domainEvent.getEvent() == null) {
            left(domainEvent.getKey());
        } else if (domainEvent.getEvent().isEvent(EventType.PLAYER_JOINED)) {
            joined((PlayerJoinedEventDto) domainEvent.getEvent());
        }
    }

    public void consumeFromPlayerActionTopic(DomainEvent domainEvent) {
        if (domainEvent.getEvent() != null && domainEvent.getEvent().isEvent(EventType.PLAYER_MOVED)) {
            get(domainEvent.getKey())
                    .ifPresent(p -> p.moved((PlayerMovedOrSpawnedEventDto) domainEvent.getEvent()));
        }
        if (domainEvent.getEvent() != null && domainEvent.getEvent().isEvent(EventType.PLAYER_DESTROYED)) {
            get(domainEvent.getKey())
                    .ifPresent(p -> p.destroyed((PlayerDestroyedEventDto) domainEvent.getEvent()));
        }
        if (domainEvent.getEvent() != null && domainEvent.getEvent().isEvent(EventType.PLAYER_SPAWNED)) {
            get(domainEvent.getKey())
                    .ifPresent(p -> p.spawned((PlayerMovedOrSpawnedEventDto) domainEvent.getEvent()));
        }
    }

    public void consumeFromBoltFiredTopic(DomainEvent domainEvent) {
        if (domainEvent.getEvent() != null && domainEvent.getEvent().isEvent(EventType.BOLT_FIRED)) {
            get(domainEvent.getKey())
                    .ifPresent(p -> p.fired((BoltFiredEventDto) domainEvent.getEvent()));
        }
    }

    public void consumeFromBoltActionTopic(DomainEvent domainEvent) {
        if (domainEvent.getEvent() != null && domainEvent.getEvent().isEvent(EventType.BOLT_EXHAUSTED)) {
            BoltExhaustedEventDto event = (BoltExhaustedEventDto) domainEvent.getEvent();
            get(event.getOwnerPlayerId())
                    .ifPresent(Player::boltExhausted);
        }
    }

}
