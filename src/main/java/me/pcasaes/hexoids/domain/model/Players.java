package me.pcasaes.hexoids.domain.model;

import pcasaes.hexoids.proto.BoltExhaustedEventDto;
import pcasaes.hexoids.proto.BoltFiredEventDto;
import pcasaes.hexoids.proto.DirectedCommand;
import pcasaes.hexoids.proto.PlayerJoinedEventDto;
import pcasaes.hexoids.proto.PlayersListCommandDto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static me.pcasaes.hexoids.domain.model.DtoUtils.DIRECTED_COMMAND_BUILDER;
import static me.pcasaes.hexoids.domain.model.DtoUtils.DTO_BUILDER;
import static me.pcasaes.hexoids.domain.model.DtoUtils.PLAYERS_LIST_BUILDER;
import static me.pcasaes.hexoids.domain.model.DtoUtils.PLAYER_BUILDER;

/**
 * The collection of Players.
 */
public class Players implements Iterable<Player> {

    static Players create(Bolts bolts, Clock clock, ScoreBoard scoreBoard) {
        return new Players(bolts, clock, scoreBoard);
    }

    /**
     * All players are maintained in this map
     */
    private final Map<EntityId, Player> playerMap = new HashMap<>(5000, 0.5f);

    /**
     * Only players connected to this node are maintained in this set
     */
    private final Set<EntityId> playerServerUpdateSet = new HashSet<>(5000, 0.5f);

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
     * @param id player's id.
     * @return If created returns the player
     */
    public Optional<Player> createPlayer(EntityId id) {
        if (playerMap.containsKey(id)) {
            return Optional.empty();
        }
        return Optional.of(createOrGet(id));
    }

    /**
     * Returns a player, creating them if they don't exist.
     *
     * @param id the player's id.
     * @return the player
     */
    public Player createOrGet(EntityId id) {
        return playerMap.computeIfAbsent(id, this::create);
    }

    /**
     * Returns a specific player if they exist.
     * <p>
     *
     * @param id player's identifier
     * @return Returns the player if they exist
     */
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

    /**
     * Requests the list of current players to be sent to a specific player
     *
     * @param requesterId player to have the current list sent to.
     */
    public void requestListOfPlayers(EntityId requesterId) {
        PlayersListCommandDto.Builder playerListBuilder = PLAYERS_LIST_BUILDER
                .clear();

        playerMap
                .values()
                .stream()
                .map(p -> p.toDtoIfJoined(PLAYER_BUILDER))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(playerListBuilder::addPlayers);

        DirectedCommand.Builder builder = DIRECTED_COMMAND_BUILDER
                .clear()
                .setPlayerId(requesterId.getGuid())
                .setPlayersList(playerListBuilder);

        GameEvents.getClientEvents().register(
                DTO_BUILDER
                        .clear()
                        .setDirectedCommand(builder)
                        .build()
        );
    }

    /**
     * Iterator of players
     *
     * @return
     */
    @Override
    public Iterator<Player> iterator() {
        return playerMap
                .values()
                .iterator();
    }

    /**
     * Stream of players
     *
     * @return
     */
    public Stream<Player> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    void joined(PlayerJoinedEventDto event) {
        Player player = createOrGet(EntityId.of(event.getPlayerId()));
        player.joined(event);
    }

    private void left(EntityId playerId) {
        Player player = createOrGet(playerId);
        playerMap.remove(playerId);
        playerServerUpdateSet.remove(playerId);
        player.left();
    }

    void consumeFromJoinTopic(DomainEvent domainEvent) {
        if (domainEvent.getEvent() == null) {
            left(EntityId.of(domainEvent.getKey()));
        } else if (domainEvent.getEvent().hasPlayerJoined()) {
            joined(domainEvent.getEvent().getPlayerJoined());
        }
    }

    void consumeFromPlayerActionTopic(DomainEvent domainEvent) {
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

    /**
     * This call marks the player is connected on this running instance.
     * This is used to to update server calculated vector positions.
     *
     * @param playerId
     */
    public void connected(EntityId playerId) {
        playerServerUpdateSet.add(playerId);
    }

    void consumeFromPlayerFiredTopic(DomainEvent domainEvent) {
        if (domainEvent.getEvent() != null && domainEvent.getEvent().hasPlayerFired()) {
            BoltFiredEventDto playerFiredEventDto = domainEvent.getEvent().getPlayerFired();
            get(EntityId.of(playerFiredEventDto.getOwnerPlayerId()))
                    .ifPresent(p -> p.fired(domainEvent.getEvent().getPlayerFired()));
        }
    }

    void consumeFromBoltActionTopic(DomainEvent domainEvent) {
        if (domainEvent.getEvent() != null && domainEvent.getEvent().hasBoltExhausted()) {
            BoltExhaustedEventDto event = domainEvent.getEvent().getBoltExhausted();
            get(EntityId.of(event.getOwnerPlayerId()))
                    .ifPresent(Player::boltExhausted);
        }
    }

    /**
     * Updates all connected player models' vector positions.
     *
     * @param timestamp the timestamp to update the players to.
     */
    void fixedUpdate(long timestamp) {
        playerServerUpdateSet
                .stream()
                .map(playerMap::get)
                .filter(Objects::nonNull)
                .forEach(p -> p.fixedUpdate(timestamp));
    }
}
