package me.pcasaes.hexoids.core.domain.model;

import me.pcasaes.hexoids.core.domain.config.Config;
import me.pcasaes.hexoids.core.domain.metrics.GameMetrics;
import me.pcasaes.hexoids.core.domain.utils.DtoUtils;
import me.pcasaes.hexoids.core.domain.utils.TrigUtil;
import me.pcasaes.hexoids.core.domain.vector.PositionVector;
import me.pcasaes.hexoids.core.domain.vector.Vector2;
import pcasaes.hexoids.proto.BoltFiredEventDto;
import pcasaes.hexoids.proto.JoinCommandDto;
import pcasaes.hexoids.proto.PlayerDestroyedEventDto;
import pcasaes.hexoids.proto.PlayerDto;
import pcasaes.hexoids.proto.PlayerJoinedEventDto;
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
public interface Player {

    /**
     * Creates an instanceof a player
     *
     * @param id         the player's id
     * @param players    the collection of all players
     * @param bolts      the collection of all bolts
     * @param clock      the game clock.
     * @param scoreBoard scoreboard
     * @return an new instance of player
     */
    static Player create(EntityId id, Players players, Bolts bolts, Clock clock, ScoreBoard scoreBoard) {
        return new Implementation(id, players, bolts, clock, scoreBoard);
    }

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
     * @param builder
     * @return
     */
    Optional<PlayerDto> toDtoIfJoined(PlayerDto.Builder builder);

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
     */
    void destroy(EntityId playerId);

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


        private static final Random RNG = new Random();

        private final EntityId id;

        private String name;

        private int ship;

        private boolean spawned;

        private long lastSpawnOrUnspawnTimestamp;

        private float angle = 0f;

        private long movedTimestamp;

        private long spawnedTimestamp;

        private int liveBolts = 0;

        private final Players players;

        private final Bolts bolts;

        private final Clock clock;

        private final ScoreBoard scoreBoard;

        private final ResetPosition resetPosition;

        private final PositionVector position;

        private Implementation(EntityId id, Players players, Bolts bolts, Clock clock, ScoreBoard scoreBoard) {
            this.players = players;
            this.bolts = bolts;
            this.clock = clock;
            this.scoreBoard = scoreBoard;
            this.id = id;

            this.ship = RNG.nextInt(12);
            this.spawned = false;
            this.lastSpawnOrUnspawnTimestamp = clock.getTime();
            this.resetPosition = ResetPosition.create(Config.get().getPlayerResetPosition());
            this.position = PositionVector.of(
                    0,
                    0,
                    0,
                    0,
                    0,
                    PLAYER_POSITION_CONFIGURATION);
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

            if (!Config.get().isBoltInertiaEnabled()) {
                boltAngle = angle;
            } else {
                Vector2 boltVector = getFiredBoltVector(boltSpeed, now);
                boltSpeed = boltVector.getMagnitude();
                boltAngle = boltVector.getAngle();
            }

            final EntityId boltId = EntityId.newId();
            GameEvents.getDomainEvents()
                    .dispatch(DomainEvent.create(
                            GameTopic.BOLT_LIFECYCLE_TOPIC.name(),
                            boltId.getId(),
                            DtoUtils
                                    .newEvent()
                                    .setPlayerFired(DtoUtils.BOLT_FIRED_BUILDER
                                            .clear()
                                            .setBoltId(boltId.getGuid())
                                            .setOwnerPlayerId(id.getGuid())
                                            .setX(position.getXat(now))
                                            .setY(position.getYat(now))
                                            .setAngle(boltAngle)
                                            .setSpeed(boltSpeed)
                                            .setStartTimestamp(now)
                                            .setTtl(Config.get().getBoltMaxDuration())
                                    )
                                    .build()
                    ));
        }

        public void fired(BoltFiredEventDto event) {
            long now = clock.getTime();
            if (Bolt.isExpired(now, event.getStartTimestamp())) {
                toBolt(event)
                        .flatMap(b -> b.updateTimestamp(now))
                        .ifPresent(Bolt::tackleBoltExhaustion);
            } else if (this.liveBolts < Config.get().getMaxBolts()) {
                toBolt(event).ifPresent(b -> this.firedNew(event, b));
            }
        }

        private void firedNew(BoltFiredEventDto event, Bolt bolt) {
            this.liveBolts++;
            bolt.fire(event);
            GameMetrics.get().getBoltFired().increment();
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
                    event.getStartTimestamp());
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
        public Optional<PlayerDto> toDtoIfJoined(PlayerDto.Builder builder) {
            if (!isJoined()) {
                return Optional.empty();
            }
            return Optional.of(builder
                    .clear()
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

        @Override
        public void join(JoinCommandDto command) {
            setName(command.getName());

            GameEvents.getDomainEvents().dispatch(
                    DomainEvent
                            .create(GameTopic.JOIN_GAME_TOPIC.name(),
                                    this.id.getId(),
                                    DtoUtils
                                            .newEvent()
                                            .setPlayerJoined(DtoUtils.PLAYER_JOINED_BUILDER
                                                    .clear()
                                                    .setPlayerId(id.getGuid())
                                                    .setShip(ship)
                                                    .setName(name))
                                            .build()
                            )
            );
        }

        @Override
        public void joined(PlayerJoinedEventDto event) {
            this.name = event.getName();
            this.ship = event.getShip();
            GameEvents
                    .getClientEvents()
                    .dispatch(DtoUtils.newDtoEvent(ev -> ev.setPlayerJoined(event)));
            GameMetrics.get().getPlayerJoined().increment();
        }

        @Override
        public void move(float moveX, float moveY, Float angle) {
            if (!this.spawned) {
                return;
            }
            long now = clock.getTime();

            boolean angleChanged = false;
            if (angle != null) {
                float a = TrigUtil.limitRotation(this.angle, angle, Config.get().getPlayerMaxAngle());
                angleChanged = a != this.angle;
                this.angle = a;
            }


            if (this.position.moveBy(moveX, moveY, now) || angleChanged) {
                fireMoveDomainEvent(now);
            }
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
            GameEvents
                    .getClientEvents()
                    .dispatch(DtoUtils.newDtoEvent(ev -> {
                        if (spawnedEvent != null) {
                            ev.setPlayerSpawned(spawnedEvent);
                        } else {
                            ev.setPlayerMoved(movedEvent);
                        }
                    }));

        }

        private void fireMoveDomainEvent(long eventTime) {
            GameEvents.getDomainEvents().dispatch(
                    DomainEvent.create(GameTopic.PLAYER_ACTION_TOPIC.name(),
                            this.id.getId(),
                            DtoUtils
                                    .newEvent()
                                    .setPlayerMoved(DtoUtils.PLAYER_MOVED_BUILDER
                                            .clear()
                                            .setPlayerId(id.getGuid())
                                            .setX(position.getX())
                                            .setY(position.getY())
                                            .setAngle(angle)
                                            .setThrustAngle(position.getVelocity().getAngle())
                                            .setVelocity(position.getVelocity().getMagnitude())
                                            .setTimestamp(eventTime)
                                    )
                                    .build())
            );
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
                    DtoUtils.newDtoEvent(ev -> ev.setPlayerLeft(DtoUtils.PLAYER_LEFT_BUILDER
                            .clear()
                            .setPlayerId(id.getGuid())))
            );
            GameMetrics.get().getPlayerLeft().increment();
        }

        @Override
        public boolean collision(PositionVector positionVector, float collisionRadius) {
            if (!this.spawned) {
                return false;
            }
            return positionVector.intersectedWith(this.position, collisionRadius);
        }

        @Override
        public void destroy(EntityId byPlayerId) {
            GameEvents.getDomainEvents().dispatch(
                    DomainEvent.create(
                            GameTopic.PLAYER_ACTION_TOPIC.name(),
                            this.id.getId(),
                            DtoUtils
                                    .newEvent()
                                    .setPlayerDestroyed(DtoUtils.PLAYER_DESTROYED_BUILDER
                                            .clear()
                                            .setPlayerId(this.id.getGuid())
                                            .setDestroyedByPlayerId(byPlayerId.getGuid())
                                            .setDestroyedTimestamp(this.clock.getTime())
                                    )
                                    .build()
                    )
            );
            this.scoreBoard.updateScore(byPlayerId, 1);
        }

        @Override
        public void destroyed(PlayerDestroyedEventDto event) {
            setSpawned(false);
            this.scoreBoard.resetScore(this.id);
            GameEvents.getClientEvents().dispatch(
                    DtoUtils.newDtoEvent(ev -> ev.setPlayerDestroyed(event))
            );
            GameMetrics.get().getPlayerDestroyed().increment();
        }

        @Override
        public void boltExhausted() {
            this.liveBolts = Math.max(0, liveBolts - 1);
            GameMetrics.get().getBoltExhausted().increment();
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
                                DtoUtils
                                        .newEvent()
                                        .setPlayerSpawned(
                                                DtoUtils.PLAYER_SPAWNED_BUILDER
                                                        .clear()
                                                        .setLocation(
                                                                DtoUtils.PLAYER_MOVED_BUILDER
                                                                        .clear()
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
                GameMetrics.get().getPlayerSpawned().increment();
            }
        }

        @Override
        public void expungeIfStalled() {
            if ((!spawned || !isJoined()) && clock.getTime() - this.lastSpawnOrUnspawnTimestamp > Config.get().getExpungeSinceLastSpawnTimeout()) {
                leave();
            }
        }

        @Override
        public void fixedUpdate(long timestamp) {
            float x = position.getX();
            float y = position.getY();
            this.position.update(timestamp);
            if (x != position.getX() || y != position.getY()) {
                fireMoveDomainEvent(timestamp);
            }
        }

        private boolean isJoined() {
            return this.name != null;
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

        static final PositionVector.Configuration PLAYER_POSITION_CONFIGURATION = new PositionVector.Configuration() {

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
                return Config.get().getInertiaDampenCoefficient();
            }
        };
    }
}
