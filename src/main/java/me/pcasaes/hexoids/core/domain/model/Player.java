package me.pcasaes.hexoids.core.domain.model;

import me.pcasaes.hexoids.core.domain.config.Config;
import me.pcasaes.hexoids.core.domain.metrics.GameMetrics;
import me.pcasaes.hexoids.core.domain.utils.TrigUtil;
import me.pcasaes.hexoids.core.domain.vector.PositionVector;
import me.pcasaes.hexoids.core.domain.vector.Vector2;
import pcasaes.hexoids.proto.BoltFiredEventDto;
import pcasaes.hexoids.proto.BoltsAvailableCommandDto;
import pcasaes.hexoids.proto.ClientPlatforms;
import pcasaes.hexoids.proto.DirectedCommand;
import pcasaes.hexoids.proto.Dto;
import pcasaes.hexoids.proto.Event;
import pcasaes.hexoids.proto.JoinCommandDto;
import pcasaes.hexoids.proto.PlayerDestroyedEventDto;
import pcasaes.hexoids.proto.PlayerDto;
import pcasaes.hexoids.proto.PlayerJoinedEventDto;
import pcasaes.hexoids.proto.PlayerLeftEventDto;
import pcasaes.hexoids.proto.PlayerMovedEventDto;
import pcasaes.hexoids.proto.PlayerSpawnedEventDto;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Random;

/**
 * A model representation of the player. This model conflates player and ship information.
 * As this model becomes richer it might be a good idea to split the two.
 * <p>
 * Most action methods have a corresponding process method in the past tense, ex:
 * fire
 * fired
 * <p>
 * The action method will generate a domain event which will be distributed to all nodes and processed in
 * the corresponding process method.
 */
public interface Player extends GameObject {

    /**
     * Creates an instanceof a player
     *
     * @param id         the player's id
     * @param players    the collection of all players
     * @param bolts      the collection of all bolts
     * @param barriers   the collection of all barriers
     * @param clock      the game clock.
     * @param scoreBoard scoreboard
     * @return an new instance of player
     */
    static Player create(EntityId id, Players players, Bolts bolts, Barriers barriers, Clock clock, ScoreBoard scoreBoard) {
        return new Implementation(id, players, bolts, barriers, clock, scoreBoard);
    }

    EntityId id();

    /**
     * Fires a bolt. Will generate a {@link BoltFiredEventDto} domain event
     */
    void fire();

    /**
     * Processes a bolt fired by this player.
     *
     * @param event
     */
    void fired(BoltFiredEventDto event);

    /**
     * Returns the number of still active bolts fired by this player
     *
     * @return
     */
    int getActiveBoltCount();

    /**
     * Returns true if this player's id matches the paramer
     *
     * @param playerId
     * @return
     */
    boolean is(EntityId playerId);

    /**
     * Generates a {@link PlayerDto} from the player's current states
     *
     * @return
     */
    Optional<PlayerDto> toDtoIfJoined();

    /**
     * The player will join the game
     *
     * @param command
     */
    void join(JoinCommandDto command);

    /**
     * Processes a player joined game event
     *
     * @param event
     */
    void joined(PlayerJoinedEventDto event);

    /**
     * Move the player by a vector
     *
     * @param moveX vector x component
     * @param moveY vector y component
     * @param angle fire direction
     */
    void move(float moveX, float moveY, Float angle);

    /**
     * Processes a player moved event
     *
     * @param event
     */
    void moved(PlayerMovedEventDto event);

    /**
     * Leaves the game
     */
    void leave();

    /**
     * Processes a player left game event.
     */
    void left();

    /**
     * Will return true if the player's ship collides with the supplied {@link PositionVector}
     *
     * @param velocityVector
     * @param collisionRadius
     * @return
     */
    boolean collision(PositionVector velocityVector, float collisionRadius);

    /**
     * This informed player destroys this player
     *
     * @param playerId
     * @param timestamp
     */
    void destroy(EntityId playerId, long timestamp);

    /**
     * Processes the destroyed player domain event
     *
     * @param event
     */
    void destroyed(PlayerDestroyedEventDto event);

    /**
     * Called whenever this player's bolts are exhausted
     */
    void boltExhausted();

    /**
     * Spawns the player
     */
    void spawn();

    /**
     * Processes the player spawned domain event
     *
     * @param event
     */
    void spawned(PlayerSpawnedEventDto event);

    /**
     * Periodically called if the player has not spawned for a certain amount of time.
     * This will cause the player to leave the game.
     */
    void expungeIfStalled();

    /**
     * Updates the player's vector position up tot he supplied timestamp.
     *
     * @param timestamp
     */
    void fixedUpdate(long timestamp);

    class Implementation implements Player {

        private static final float SHIP_LENGTH = 0.003F;

        private static final float SHIP_HALF_LENGTH = SHIP_LENGTH / 2F;

        private static final float SHIP_LENGTH_TIMES_10 = SHIP_LENGTH * 10F;

        private static final Random RNG = new Random();

        private final EntityId id;

        private String name;

        private ClientPlatforms clientPlatform;

        private int ship;

        private boolean spawned;

        private long lastSpawnOrUnspawnTimestamp;

        private float previousAngle = 0F;

        private float angle = 0F;

        private long movedTimestamp;

        private long spawnedTimestamp;

        private int liveBolts = 0;

        private final Players players;

        private final Bolts bolts;

        private final Barriers barriers;

        private final Clock clock;

        private final ScoreBoard scoreBoard;

        private final ResetPosition resetPosition;

        private final PositionVector position;

        private final PlayerPositionConfiguration playerPositionConfiguration;

        private Implementation(EntityId id, Players players, Bolts bolts, Barriers barriers, Clock clock, ScoreBoard scoreBoard) {
            this.players = players;
            this.bolts = bolts;
            this.clock = clock;
            this.barriers = barriers;
            this.scoreBoard = scoreBoard;
            this.id = id;

            this.ship = RNG.nextInt(12);
            this.spawned = false;
            this.lastSpawnOrUnspawnTimestamp = clock.getTime();
            this.resetPosition = ResetPosition.create(Config.get().getPlayerResetPosition());
            this.playerPositionConfiguration = new PlayerPositionConfiguration();
            this.position = PositionVector.of(
                    0,
                    0,
                    0,
                    0,
                    0,
                    playerPositionConfiguration);
        }

        @Override
        public EntityId id() {
            return this.id;
        }

        @Override
        public void setDampenMovementFactorUntilNextFixedUpdate(float factor) {
            this.playerPositionConfiguration.setDampenFactor(factor);
        }

        private void setSpawned(boolean spawned) {
            this.spawned = spawned;
            this.lastSpawnOrUnspawnTimestamp = clock.getTime();
        }

        private Vector2 getFiredBoltVector(float boltSpeed, long firedTime) {
            Vector2 boltVector = Vector2.fromAngleMagnitude(this.angle, boltSpeed);

            Vector2 currentShipVector = this.position.getVectorAt(firedTime);
            Vector2 projection = currentShipVector
                    .projection(boltVector);

            Vector2 rejection = currentShipVector
                    .minus(projection)
                    .scale(Config.get().getBoltInertiaRejectionScale());

            if (boltVector.sameDirection(projection)) {
                projection = projection.scale(Config.get().getBoltInertiaProjectionScale());
            } else {
                projection = projection.scale(Config.get().getBoltInertiaNegativeProjectionScale());
            }
            projection = projection
                    .scale(Config.get().getBoltInertiaProjectionScale());

            return boltVector.add(rejection).add(projection);
        }

        @Override
        public void fire() {
            if (!spawned) {
                return;
            }

            long now = clock.getTime();

            float boltSpeed = Config.get().getBoltSpeed();
            float boltAngle;

            Vector2 boltVector;
            if (!Config.get().isBoltInertiaEnabled()) {
                boltAngle = angle;
                boltVector = Vector2.fromAngleMagnitude(boltAngle, boltSpeed);
            } else {
                boltVector = getFiredBoltVector(boltSpeed, now);
                boltSpeed = boltVector.getMagnitude();
                boltAngle = boltVector.getAngle();
            }


            Vector2 positionAtNow = Vector2.fromXY(
                    position.getXat(now),
                    position.getYat(now)
            );

            int ttl = calculateBoltTll(positionAtNow, boltVector, boltSpeed);


            final EntityId boltId = EntityId.newId();
            GameEvents.getDomainEvents()
                    .dispatch(DomainEvent.create(
                            GameTopic.BOLT_LIFECYCLE_TOPIC.name(),
                            boltId.getId(),
                            Event.newBuilder()
                                    .setPlayerFired(BoltFiredEventDto.newBuilder()
                                            .setBoltId(boltId.getGuid())
                                            .setOwnerPlayerId(id.getGuid())
                                            .setX(positionAtNow.getX())
                                            .setY(positionAtNow.getY())
                                            .setAngle(boltAngle)
                                            .setSpeed(boltSpeed)
                                            .setStartTimestamp(now)
                                            .setTtl(ttl)
                                    )
                                    .build()
                    ));
        }

        private int calculateBoltTll(Vector2 positionAtNow, Vector2 boltVector, float boltSpeed) {
            int maxTtl = Config.get().getBoltMaxDuration();

            Vector2 moveDelta = Vector2.calculateMoveDelta(boltVector, Float.MIN_VALUE, maxTtl);

            Vector2 positionAtEnd = positionAtNow.add(moveDelta);

            float magnitudeForTtl = -1;

            for (Barrier barrier : barriers
                    .search(positionAtNow.getX(), positionAtNow.getY(), positionAtEnd.getX(), positionAtEnd.getY(), SHIP_LENGTH_TIMES_10)) {
                Vector2 intersection = Vector2.intersectedWith(positionAtNow, positionAtEnd, barrier.getTo(), barrier.getFrom());
                if (intersection != null) {
                    if (magnitudeForTtl == -1) {
                        magnitudeForTtl = intersection.minus(positionAtNow).getMagnitude();
                    } else {
                        float m = intersection.minus(positionAtNow).getMagnitude();
                        if (m < magnitudeForTtl) {
                            magnitudeForTtl = m;
                        }
                    }
                }
            }

            if (magnitudeForTtl != -1) {
                return Math.min(maxTtl, (int) (1000F * magnitudeForTtl / boltSpeed));
            }
            return maxTtl;
        }

        public void fired(BoltFiredEventDto event) {
            long now = clock.getTime();
            if (Bolt.isExpired(now, event.getStartTimestamp(), event.getTtl())) {
                toBolt(event)
                        .flatMap(b -> b.updateTimestamp(now))
                        .ifPresent(b -> b.tackleBoltExhaustion(now));
            } else if (this.liveBolts < Config.get().getMaxBolts()) {
                toBolt(event).ifPresent(b -> this.firedNew(event, b));
            }
        }

        private void firedNew(BoltFiredEventDto event, Bolt bolt) {
            this.liveBolts++;
            bolt.fire(event);
            liveBoltsChanged();
            GameMetrics.get().getBoltFired().increment(this.clientPlatform);
        }

        private Optional<Bolt> toBolt(BoltFiredEventDto event) {
            return this.bolts.fired(
                    players,
                    EntityId.of(event.getBoltId()),
                    this.id,
                    event.getX(),
                    event.getY(),
                    event.getAngle(),
                    event.getSpeed(),
                    event.getStartTimestamp(),
                    event.getTtl());
        }


        @Override
        public int getActiveBoltCount() {
            return this.liveBolts;
        }

        @Override
        public boolean is(EntityId playerId) {
            return id.equals(playerId);
        }

        @Override
        public Optional<PlayerDto> toDtoIfJoined() {
            if (!isJoined()) {
                return Optional.empty();
            }
            return Optional.of(PlayerDto.newBuilder()
                    .setPlayerId(id.getGuid())
                    .setShip(ship)
                    .setX(position.getX())
                    .setY(position.getY())
                    .setAngle(angle)
                    .setSpawned(spawned)
                    .setName(name)
                    .build());
        }

        private void resetPosition(long resetTime) {
            position.initialized(resetPosition.getNextX(), resetPosition.getNextY(), resetTime);
            this.angle = 0f;
        }

        private void setName(String n) {
            n = (n == null || n.length() == 0) ?
                    id.getId().toString() :
                    n;
            if (n.length() > Config.get().getPlayerNameLength()) {
                n = n.substring(0, Config.get().getPlayerNameLength());
            }
            this.name = n;
        }

        public void setClientPlatform(ClientPlatforms clientPlatform) {
            this.clientPlatform = clientPlatform;
        }

        @Override
        public void join(JoinCommandDto command) {
            setName(command.getName());
            setClientPlatform(command.getClientPlatform());

            GameEvents.getDomainEvents().dispatch(
                    DomainEvent
                            .create(GameTopic.JOIN_GAME_TOPIC.name(),
                                    this.id.getId(),
                                    Event.newBuilder()
                                            .setPlayerJoined(PlayerJoinedEventDto.newBuilder()
                                                    .setPlayerId(id.getGuid())
                                                    .setShip(ship)
                                                    .setName(name)
                                                    .setClientPlatform(clientPlatform)
                                            )
                                            .build()
                            )
            );
        }

        @Override
        public void joined(PlayerJoinedEventDto event) {
            this.name = event.getName();
            this.clientPlatform = event.getClientPlatform();
            this.ship = event.getShip();
            GameEvents
                    .getClientEvents()
                    .dispatch(Dto.newBuilder()
                            .setEvent(Event.newBuilder()
                                    .setPlayerJoined(PlayerJoinedEventDto.newBuilder()
                                            .mergeFrom(event)
                                            .clearClientPlatform()  //let's not publish to all clients user data
                                    )
                            ).build());
            GameMetrics.get().getPlayerJoined().increment(this.clientPlatform);
        }

        @Override
        public void move(float moveX, float moveY) {
            move(moveX, moveY, null);
        }

        @Override
        public void move(float moveX, float moveY, Float angle) {
            if (!this.spawned) {
                return;
            }

            if (angle != null) {
                this.previousAngle = this.angle;
                this.angle = TrigUtil.limitRotation(this.angle, angle, Config.get().getPlayerMaxAngle());
            }

            position.scheduleMove(moveX, moveY);
        }

        @Override
        public void moved(PlayerMovedEventDto event) {
            if (event.getTimestamp() > this.movedTimestamp) {
                this.movedTimestamp = event.getTimestamp();
                movedOrSpawned(event, null);
            }
        }

        private void movedOrSpawned(PlayerMovedEventDto movedEvent, PlayerSpawnedEventDto spawnedEvent) {
            this.position.moved(
                    movedEvent.getX(),
                    movedEvent.getY(),
                    movedEvent.getThrustAngle(),
                    movedEvent.getVelocity(),
                    movedEvent.getTimestamp());
            this.angle = movedEvent.getAngle();

            Event.Builder eventBuilder = Event.newBuilder();
            if (spawnedEvent != null) {
                eventBuilder.setPlayerSpawned(spawnedEvent);
            } else {
                eventBuilder.setPlayerMoved(movedEvent);
            }
            GameEvents
                    .getClientEvents()
                    .dispatch(
                            Dto.newBuilder()
                                    .setEvent(eventBuilder)
                                    .build()
                    );

        }

        private void fireMoveDomainEvent(long eventTime) {
            GameEvents.getDomainEvents().dispatch(
                    DomainEvent.create(GameTopic.PLAYER_ACTION_TOPIC.name(),
                            this.id.getId(),
                            Event.newBuilder()
                                    .setPlayerMoved(
                                            PlayerMovedEventDto.newBuilder()
                                                    .setPlayerId(id.getGuid())
                                                    .setX(position.getX())
                                                    .setY(position.getY())
                                                    .setAngle(angle)
                                                    .setThrustAngle(position.getVelocity().getAngle())
                                                    .setVelocity(position.getVelocity().getMagnitude())
                                                    .setTimestamp(eventTime)

                                    ).build()));
        }

        @Override
        public void leave() {
            GameEvents.getDomainEvents().dispatch(DomainEvent.delete(GameTopic.JOIN_GAME_TOPIC.name(), this.id.getId()));
            GameEvents.getDomainEvents().dispatch(DomainEvent.delete(GameTopic.PLAYER_ACTION_TOPIC.name(), this.id.getId()));
        }

        @Override
        public void left() {
            scoreBoard.resetScore(this.id);
            GameEvents.getClientEvents().dispatch(
                    Dto.newBuilder()
                            .setEvent(Event.newBuilder()
                                    .setPlayerLeft(PlayerLeftEventDto.newBuilder().setPlayerId(id.getGuid()))
                            )
                            .build());
            GameMetrics.get().getPlayerLeft().increment(this.clientPlatform);
            this.spawned = false;
        }

        @Override
        public boolean collision(PositionVector positionVector, float collisionRadius) {
            if (!this.spawned) {
                return false;
            }
            return positionVector.intersectedWith(this.position, collisionRadius);
        }

        @Override
        public void destroy(EntityId byPlayerId, long timestamp) {
            if (spawned) {
                hazardDestroy(byPlayerId, timestamp);
                this.scoreBoard.updateScore(byPlayerId, 1);
            }
        }

        @Override
        public void hazardDestroy(EntityId hazardId, long timestamp) {
            if (spawned) {
                GameEvents.getDomainEvents().dispatch(
                        DomainEvent.create(
                                GameTopic.PLAYER_ACTION_TOPIC.name(),
                                this.id.getId(),
                                Event.newBuilder()
                                        .setPlayerDestroyed(PlayerDestroyedEventDto.newBuilder()
                                                .setPlayerId(this.id.getGuid())
                                                .setDestroyedById(hazardId.getGuid())
                                                .setDestroyedTimestamp(timestamp))
                                        .build()
                        )
                );
            }
        }

        @Override
        public void destroyed(PlayerDestroyedEventDto event) {
            setSpawned(false);
            this.scoreBoard.resetScore(this.id);
            GameEvents.getClientEvents().dispatch(
                    Dto.newBuilder()
                            .setEvent(Event.newBuilder().setPlayerDestroyed(event))
                            .build()
            );
            GameMetrics.get().getPlayerDestroyed().increment(this.clientPlatform);
        }

        @Override
        public void boltExhausted() {
            this.liveBolts = Math.max(0, liveBolts - 1);
            liveBoltsChanged();
            GameMetrics.get().getBoltExhausted().increment(this.clientPlatform);
        }

        private void liveBoltsChanged() {
            if (players.isConnected(id)) {
                BoltsAvailableCommandDto.Builder dto = BoltsAvailableCommandDto.newBuilder()
                        .setAvailable(Math.max(0, Config.get().getMaxBolts() - this.liveBolts));

                DirectedCommand.Builder builder = DirectedCommand.newBuilder()
                        .setPlayerId(id.getGuid())
                        .setBoltsAvailable(dto);

                GameEvents.getClientEvents().dispatch(
                        Dto.newBuilder()
                                .setDirectedCommand(builder)
                                .build()
                );
            }
        }

        @Override
        public void spawn() {
            if (!this.spawned) {
                setSpawned(true);
                long now = clock.getTime();
                resetPosition(now);
                GameEvents.getDomainEvents().dispatch(
                        DomainEvent.create(GameTopic.PLAYER_ACTION_TOPIC.name(),
                                this.id.getId(),
                                Event.newBuilder()
                                        .setPlayerSpawned(PlayerSpawnedEventDto.newBuilder()
                                                .setLocation(
                                                        PlayerMovedEventDto.newBuilder()
                                                                .setPlayerId(id.getGuid())
                                                                .setX(position.getX())
                                                                .setY(position.getY())
                                                                .setAngle(angle)
                                                                .setThrustAngle(position.getVelocity().getAngle())
                                                                .setTimestamp(now)
                                                )
                                        )
                                        .build()
                        )
                );
            }
        }

        @Override
        public void spawned(PlayerSpawnedEventDto event) {
            if (event.getLocation().getTimestamp() > this.spawnedTimestamp) {
                this.spawnedTimestamp = event.getLocation().getTimestamp();
                setSpawned(true);
                this.position.initialized(event.getLocation().getX(),
                        event.getLocation().getY(),
                        0L);
                movedOrSpawned(event.getLocation(), event);
                GameMetrics.get().getPlayerSpawned().increment(this.clientPlatform);
            }
        }

        @Override
        public void expungeIfStalled() {
            if ((!spawned || !isJoined()) && clock.getTime() - this.lastSpawnOrUnspawnTimestamp > Config.get().getExpungeSinceLastSpawnTimeout()) {
                leave();
                GameMetrics.get().getPlayerStalled().increment(this.clientPlatform);
            }
        }

        @Override
        public void fixedUpdate(long timestamp) {
            float x = position.getX();
            float y = position.getY();
            this.position.update(timestamp);
            boolean angleChanged = this.previousAngle != this.angle;
            if (x != position.getX() || y != position.getY()) {
                tackleBarrierHit();
                fireMoveDomainEvent(timestamp);
            } else if (angleChanged) {
                fireMoveDomainEvent(timestamp);
            }
            if (angleChanged) {
                this.previousAngle = this.angle;
            }
            this.playerPositionConfiguration.setDampenFactor(1F);
        }

        private void tackleBarrierHit() {
            if (!position.noMovement()) {
                for (Barrier barrier : barriers
                        .search(position.getPreviousX(), position.getPreviousY(), position.getX(), position.getY(), SHIP_LENGTH_TIMES_10)) {
                    Vector2 intersection = position.intersectedWith(barrier.getTo(), barrier.getFrom(), SHIP_HALF_LENGTH);
                    if (intersection != null) {
                        position.reflect(intersection, barrier.getNormal(), SHIP_HALF_LENGTH, 0.5F);
                    }
                }
            }
        }

        private boolean isJoined() {
            return this.name != null;
        }

        @Override
        public float getX() {
            return this.position.getX();
        }

        @Override
        public float getY() {
            return this.position.getY();
        }

        @Override
        public ClientPlatforms getClientPlatform() {
            return clientPlatform;
        }

        @Override
        public boolean supportsInertialDampener() {
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Implementation that = (Implementation) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        static class PlayerPositionConfiguration implements PositionVector.Configuration {

            private float dampenFactor = 1F;

            public void setDampenFactor(float dampenFactor) {
                this.dampenFactor = dampenFactor;
            }

            @Override
            public AtBoundsOptions atBounds() {
                return AtBoundsOptions.BOUNCE;
            }

            @Override
            public OptionalDouble maxMagnitude() {
                return OptionalDouble.of(Config.get().getPlayerMaxMove());
            }

            @Override
            public float dampenMagnitudeCoefficient() {
                return Config.get().getInertiaDampenCoefficient() * dampenFactor;
            }
        }
    }
}
