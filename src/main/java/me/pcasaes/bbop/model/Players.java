package me.pcasaes.bbop.model;

import me.pcasaes.bbop.model.annotations.IsThreadSafe;
import me.pcasaes.bbop.util.concurrent.SingleMutatorMultipleAccessorConcurrentHashMap;
import pcasaes.bbop.proto.BoltExhaustedEventDto;
import pcasaes.bbop.proto.BoltFiredEventDto;
import pcasaes.bbop.proto.DirectedCommand;
import pcasaes.bbop.proto.PlayerDto;
import pcasaes.bbop.proto.PlayerJoinedEventDto;
import pcasaes.bbop.proto.PlayersListCommandDto;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static me.pcasaes.bbop.model.DtoUtils.DIRECTED_COMMAND_THREAD_SAFE_BUILDER;
import static me.pcasaes.bbop.model.DtoUtils.DTO_THREAD_SAFE_BUILDER;
import static me.pcasaes.bbop.model.DtoUtils.PLAYERS_LIST_THREAD_SAFE_BUILDER;
import static me.pcasaes.bbop.model.DtoUtils.PLAYER_THREAD_SAFE_BUILDER;

public class Players implements Iterable<Player> {

    static Players create(Bolts bolts, Clock clock, ScoreBoard scoreBoard) {
        return new Players(bolts, clock, scoreBoard);
    }

    private final Map<EntityId, Player> playerMap = new SingleMutatorMultipleAccessorConcurrentHashMap<>(5000, 0.5f);

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
    public Optional<Player> createPlayer(EntityId id) {
        if (playerMap.containsKey(id)) {
            return Optional.empty();
        }
        return Optional.of(createOrGet(id));
    }

    public Player createOrGet(EntityId id) {
        return playerMap.computeIfAbsent(id, this::create);
    }

    @IsThreadSafe
    public Optional<Player> get(EntityId id) {
        return Optional.ofNullable(playerMap.get(id));
    }

    private Optional<Player> get(UUID id) {
        return get(EntityId.of(id));
    }

    private Player create(EntityId id) {
        requestListOfPlayers(id);
        return Player.create(id, this, this.bolts, this.clock, this.scoreBoard);
    }

    @IsThreadSafe
    public void requestListOfPlayers(EntityId requesterId) {
        PlayersListCommandDto.Builder playerListBuilder = PLAYERS_LIST_THREAD_SAFE_BUILDER
                .get()
                .clear();

        PlayerDto.Builder playerBuilder = PLAYER_THREAD_SAFE_BUILDER.get();
        playerMap
                .values()
                .stream()
                .map(p -> p.toDto(playerBuilder))
                .forEach(playerListBuilder::addPlayers);

        DirectedCommand.Builder builder = DIRECTED_COMMAND_THREAD_SAFE_BUILDER
                .get()
                .clear()
                .setPlayerId(requesterId.getGuid())
                .setPlayersList(playerListBuilder);

        GameEvents.getClientEvents().register(
                DTO_THREAD_SAFE_BUILDER
                        .get()
                        .clear()
                        .setDirectedCommand(builder)
                        .build()
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
        Player player = createOrGet(EntityId.of(event.getPlayerId()));
        player.joined(event);
    }

    void left(EntityId playerId) {
        Player player = createOrGet(playerId);
        playerMap.remove(playerId);
        player.left();
    }

    public void consumeFromJoinTopic(DomainEvent domainEvent) {
        if (domainEvent.getEvent() == null) {
            left(EntityId.of(domainEvent.getKey()));
        } else if (domainEvent.getEvent().hasPlayerJoined()) {
            joined(domainEvent.getEvent().getPlayerJoined());
        }
    }

    public void consumeFromPlayerActionTopic(DomainEvent domainEvent) {
        if (domainEvent.getEvent() != null && domainEvent.getEvent().hasPlayerMoved()) {
            get(domainEvent.getKey())
                    .ifPresent(p -> p.moved(domainEvent.getEvent().getPlayerMoved()));
        }
        if (domainEvent.getEvent() != null && domainEvent.getEvent().hasPlayerDestroyed()) {
            get(domainEvent.getKey())
                    .ifPresent(p -> p.destroyed(domainEvent.getEvent().getPlayerDestroyed()));
        }
        if (domainEvent.getEvent() != null && domainEvent.getEvent().hasPlayerSpawned()) {
            get(domainEvent.getKey())
                    .ifPresent(p -> p.spawned(domainEvent.getEvent().getPlayerSpawned()));
        }
    }

    public void consumeFromBoltFiredTopic(DomainEvent domainEvent) {
        if (domainEvent.getEvent() != null && domainEvent.getEvent().hasBoltFired()) {
            BoltFiredEventDto boltFiredEventDto = domainEvent.getEvent().getBoltFired();
            get(EntityId.of(boltFiredEventDto.getOwnerPlayerId()))
                    .ifPresent(p -> p.fired(domainEvent.getEvent().getBoltFired()));
        }
    }

    public void consumeFromBoltActionTopic(DomainEvent domainEvent) {
        if (domainEvent.getEvent() != null && domainEvent.getEvent().hasBoltExhausted()) {
            BoltExhaustedEventDto event = domainEvent.getEvent().getBoltExhausted();
            get(EntityId.of(event.getOwnerPlayerId()))
                    .ifPresent(Player::boltExhausted);
        }
    }

}
