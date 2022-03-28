package me.pcasaes.hexoids.core.domain.model;

import me.pcasaes.hexoids.core.domain.config.Config;
import me.pcasaes.hexoids.core.domain.index.PlayerSpatialIndex;
import me.pcasaes.hexoids.core.domain.index.PlayerSpatialIndexFactory;
import me.pcasaes.hexoids.core.domain.utils.MathUtil;
import me.pcasaes.hexoids.core.domain.vector.Vector2;
import pcasaes.hexoids.proto.BoltExhaustedEventDto;
import pcasaes.hexoids.proto.BoltFiredEventDto;
import pcasaes.hexoids.proto.BoltsAvailableCommandDto;
import pcasaes.hexoids.proto.CurrentViewCommandDto;
import pcasaes.hexoids.proto.DirectedCommand;
import pcasaes.hexoids.proto.Dto;
import pcasaes.hexoids.proto.PlayerDestroyedEventDto;
import pcasaes.hexoids.proto.PlayerJoinedEventDto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The collection of Players.
 */
public class Players implements Iterable<Player> {

    static Players create(Bolts bolts, Clock clock, ScoreBoard scoreBoard, Barriers barriers, PlayerSpatialIndexFactory spatialIndexFactory) {
        return new Players(bolts, clock, scoreBoard, barriers, spatialIndexFactory);
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

    private final Barriers barriers;

    private final PlayerSpatialIndexFactory spatialIndexFactory;

    private Players(Bolts bolts, Clock clock, ScoreBoard scoreBoard, Barriers barriers, PlayerSpatialIndexFactory spatialIndexFactory) {
        this.bolts = bolts;
        this.clock = clock;
        this.scoreBoard = scoreBoard;
        this.barriers = barriers;
        this.spatialIndexFactory = spatialIndexFactory;
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
        requestCurrentView(id);
        return Player.create(id, this, this.bolts, this.barriers, this.clock, this.scoreBoard);
    }

    /**
     * Requests the game's current view to be sent to a specific player
     *
     * @param requesterId player to have the current view sent to.
     */
    public void requestCurrentView(EntityId requesterId) {
        CurrentViewCommandDto.Builder currentViewBuilder = CurrentViewCommandDto.newBuilder()
                .addAllBarriers(StreamSupport.stream(barriers.spliterator(), false)
                        .map(Barrier::toDto)
                        .collect(Collectors.toList())
                )
                .setBoltsAvailable(BoltsAvailableCommandDto.newBuilder().setAvailable(Config.get().getMaxBolts()));

        playerMap
                .values()
                .stream()
                .map(Player::toDtoIfJoined)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(currentViewBuilder::addPlayers);

        DirectedCommand.Builder builder = DirectedCommand.newBuilder()
                .setPlayerId(requesterId.getGuid())
                .setCurrentView(currentViewBuilder);

        GameEvents.getClientEvents().dispatch(
                Dto.newBuilder()
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
                    .ifPresent(p -> handleDestroyed(p, domainEvent.getEvent().getPlayerDestroyed()));
        }
        if (domainEvent.getEvent() != null && domainEvent.getEvent().hasPlayerSpawned()) {
            get(domainEvent.getKey())
                    .ifPresent(p -> p.spawned(domainEvent.getEvent().getPlayerSpawned()));
        }
    }

    void handleDestroyed(Player player, PlayerDestroyedEventDto playerDestroyedEvent) {
        handleShockwave(player);
        player.destroyed(playerDestroyedEvent);
    }

    /**
     * When a player is destroyed we generate a shockwave that pushes nearby players away
     * from the destroyed players last position.
     *
     * @param fromPlayer
     */
    void handleShockwave(Player fromPlayer) {
        final var dist = Config.get().getPlayerDestroyedShockwaveDistance();
        if (dist <= 0F) {
            return;
        }
        Vector2 fromPlayerPosition = Vector2.fromXY(fromPlayer.getX(), fromPlayer.getY());
        getSpatialIndex()
                .search(fromPlayer.getX(), fromPlayer.getY(), fromPlayer.getX(), fromPlayer.getY(), dist)
                .forEach(nearPlayer -> {
                    if (nearPlayer != fromPlayer) {
                        Vector2 distanceBetweenPlayers = Vector2
                                .fromXY(nearPlayer.getX(), nearPlayer.getY())
                                .minus(fromPlayerPosition);

                        // rescaled to be between 0.0 and 1.0 inclusive
                        float absMagRescaled = Math.abs(distanceBetweenPlayers.getMagnitude()) / dist;

                        boolean isNotCenteredNorOutOfRange = absMagRescaled <= 1F && absMagRescaled > 0F;
                        if (isNotCenteredNorOutOfRange) {
                            int sign = distanceBetweenPlayers.getMagnitude() < 0F ? -1 : 1;

                            float invertedAbsMag = (1F - absMagRescaled);
                            if (invertedAbsMag > 0.5) {
                                // if near destroyed player let's limit non-linearly (quadratic) the shockwave magnitude
                                // f(x) =   0.75 - (x - 1)^2    : x > 0.5
                                //          x                   : x <= 0.5
                                // at 0.5 the quadratic transform intersects tangentially to f(x) = x
                                invertedAbsMag = 0.75F - MathUtil.square(invertedAbsMag - 1F);
                            }

                            Vector2 move = Vector2
                                    .fromAngleMagnitude(
                                            distanceBetweenPlayers.getAngle(),
                                            sign * invertedAbsMag * dist
                                    );

                            nearPlayer.move(move.getX(), move.getY(), null);
                        }
                    }
                });
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

    /**
     * Return the total number of players in the game.
     * Is weakly consistent and thread safe.
     *
     * @return
     */
    public int getTotalNumberOfPlayers() {
        return this.playerMap.size();
    }

    /**
     * Return the number of players connected this node.
     * Is weakly consistent and thread safe.
     *
     * @return
     */
    public int getNumberOfConnectedPlayers() {
        return this.playerServerUpdateSet.size();
    }

    PlayerSpatialIndex getSpatialIndex() {
        return spatialIndexFactory.get();
    }

    boolean isConnected(EntityId id) {
        return playerServerUpdateSet.contains(id);
    }
}
