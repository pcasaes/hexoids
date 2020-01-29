package me.pcasaes.bbop.model;

import me.pcasaes.bbop.model.annotations.IsThreadSafe;
import me.pcasaes.bbop.model.vector.PositionVector;
import me.pcasaes.bbop.model.vector.Vector2;
import me.pcasaes.bbop.util.TrigUtil;
import pcasaes.bbop.proto.BoltFiredEventDto;
import pcasaes.bbop.proto.PlayerDestroyedEventDto;
import pcasaes.bbop.proto.PlayerDto;
import pcasaes.bbop.proto.PlayerJoinedEventDto;
import pcasaes.bbop.proto.PlayerMovedEventDto;
import pcasaes.bbop.proto.PlayerSpawnedEventDto;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Random;

import static me.pcasaes.bbop.model.DtoUtils.BOLT_FIRED_BUILDER;
import static me.pcasaes.bbop.model.DtoUtils.PLAYER_DESTROYED_BUILDER;
import static me.pcasaes.bbop.model.DtoUtils.PLAYER_JOINED_BUILDER;
import static me.pcasaes.bbop.model.DtoUtils.PLAYER_LEFT_BUILDER;
import static me.pcasaes.bbop.model.DtoUtils.PLAYER_MOVED_BUILDER;
import static me.pcasaes.bbop.model.DtoUtils.PLAYER_SPAWNED_BUILDER;
import static me.pcasaes.bbop.model.DtoUtils.newDtoEvent;

public interface Player {

    static Player create(EntityId id, Players players, Bolts bolts, Clock clock, ScoreBoard scoreBoard) {
        return new Implementation(id, players, bolts, clock, scoreBoard);
    }

    void fire();

    void fired(BoltFiredEventDto event);

    int getActiveBoltCount();

    boolean is(EntityId playerId);

    PlayerDto toDto(PlayerDto.Builder builder);

    void join();

    void joined(PlayerJoinedEventDto event);

    void move(float moveX, float moveY, Float angle);

    void moved(PlayerMovedEventDto event);

    void leave();

    void left();

    boolean collision(PositionVector velocityVector, float collisionRadius);

    void destroy(EntityId playerId);

    void destroyed(PlayerDestroyedEventDto event);

    void boltExhausted();

    void spawn();

    void spawned(PlayerSpawnedEventDto event);

    void expungeIfStalled();

    class Implementation implements Player {


        private static final Random RNG = new Random();

        private final EntityId id;

        private int ship;

        private boolean spawned;

        private long lastSpawnOrUnspawnTimestamp;

        private float angle = 0f;

        private long movedTimestamp;

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

            this.ship = RNG.nextInt(6);
            setSpawned(false);
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

        private Vector2 getFiredBoltVector(float boltSpeed, long timeSinceInertiaUpdate) {
            Vector2 boltVector = Vector2.fromAngleMagnitude(this.angle, boltSpeed);

            final float dampenRatio = timeSinceInertiaUpdate / (float) Config.get().getInertiaDampenTimeMillis();
            final float converseDampenRatioSquared = 1f - dampenRatio * dampenRatio;

            Vector2 scaledInertia = this.position
                    .getVector()
                    .scale(converseDampenRatioSquared);
            Vector2 projection = scaledInertia
                    .projection(boltVector);

            Vector2 rejection = scaledInertia
                    .minus(projection)
                    .absMax(Config.get().getBoltInertiaRejectionMax());

            projection = projection
                    .absMax(Config.get().getBoltInertiaProjectionMax());

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

            long timeSinceInertiaUpdate = now - position.getTimestamp();
            if (!Config.get().isBoltInertiaEnabled() ||
                    timeSinceInertiaUpdate >= Config.get().getInertiaDampenTimeMillis() ||
                    timeSinceInertiaUpdate < 0) {
                boltAngle = angle;
            } else {
                Vector2 boltVector = getFiredBoltVector(boltSpeed, timeSinceInertiaUpdate);
                boltSpeed = boltVector.getMagnitude();
                boltAngle = boltVector.getAngle();
            }

            final EntityId boltId = EntityId.newId();
            GameEvents.getDomainEvents()
                    .register(DomainEvent.create(
                            Topics.BOLT_LIFECYCLE_TOPIC.name(),
                            boltId.getId(),
                            DtoUtils
                                    .newEvent()
                                    .setBoltFired(BOLT_FIRED_BUILDER
                                            .clear()
                                            .setBoltId(boltId.getGuid())
                                            .setOwnerPlayerId(id.getGuid())
                                            .setX(position.getX())
                                            .setY(position.getY())
                                            .setAngle(boltAngle)
                                            .setSpeed(boltSpeed)
                                            .setStartTimestamp(now)
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
                toBolt(event).ifPresent(b -> this.liveBolts++);
            }
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
        public PlayerDto toDto(PlayerDto.Builder builder) {
            return builder
                    .clear()
                    .setPlayerId(id.getGuid())
                    .setShip(ship)
                    .setX(position.getX())
                    .setY(position.getY())
                    .setAngle(angle)
                    .setSpawned(spawned)
                    .build();
        }

        private void resetPosition(long resetTime) {
            position.initialized(resetPosition.getNextX(), resetPosition.getNextY(), resetTime);
            this.angle = 0f;
        }

        @Override
        public void join() {
            GameEvents.getDomainEvents().register(
                    DomainEvent
                            .create(Topics.JOIN_GAME_TOPIC.name(),
                                    this.id.getId(),
                                    DtoUtils
                                            .newEvent()
                                            .setPlayerJoined(PLAYER_JOINED_BUILDER
                                                    .clear()
                                                    .setPlayerId(id.getGuid())
                                                    .setShip(ship))
                                            .build()
                            )
            );
        }

        @Override
        public void joined(PlayerJoinedEventDto event) {
            this.ship = event.getShip();
            GameEvents
                    .getClientEvents()
                    .register(newDtoEvent(ev -> ev.setPlayerJoined(event)));
        }

        @Override
        public void move(float moveX, float moveY, Float angle) {
            if (!this.spawned) {
                return;
            }

            boolean angleChanged = false;
            if (angle != null) {
                float a = TrigUtil.limitRotation(this.angle, angle, Config.get().getPlayerMaxAngle());
                angleChanged = a != this.angle;
                this.angle = a;
            }


            long now = clock.getTime();
            if (this.position.moveBy(moveX, moveY, now) || angleChanged) {
                fireMoveDomainEvent(now);
            }
        }

        @Override
        public void moved(PlayerMovedEventDto event) {
            movedOrSpawned(event, null);
        }

        private void movedOrSpawned(PlayerMovedEventDto movedEvent, PlayerSpawnedEventDto spawnedEvent) {
            if (movedEvent.getTimestamp() > this.movedTimestamp) {
                this.position.moved(
                        movedEvent.getX(),
                        movedEvent.getY(),
                        movedEvent.getThrustAngle(),
                        movedEvent.getVelocity(),
                        movedEvent.getTimestamp());
                this.angle = movedEvent.getAngle();
                this.movedTimestamp = movedEvent.getTimestamp();
                GameEvents
                        .getClientEvents()
                        .register(newDtoEvent(ev -> {
                            if (spawnedEvent != null) {
                                ev.setPlayerSpawned(spawnedEvent);
                            } else {
                                ev.setPlayerMoved(movedEvent);
                            }
                        }));
            }
        }

        private void fireMoveDomainEvent(long eventTime) {
            GameEvents.getDomainEvents().register(
                    DomainEvent.create(Topics.PLAYER_ACTION_TOPIC.name(),
                            this.id.getId(),
                            DtoUtils
                                    .newEvent()
                                    .setPlayerMoved(PLAYER_MOVED_BUILDER
                                            .clear()
                                            .setPlayerId(id.getGuid())
                                            .setX(position.getX())
                                            .setY(position.getY())
                                            .setAngle(angle)
                                            .setThrustAngle(position.getVector().getAngle())
                                            .setVelocity(position.getVector().getMagnitude())
                                            .setTimestamp(eventTime)
                                    )
                                    .build())
            );
        }

        @Override
        @IsThreadSafe
        public void leave() {
            GameEvents.getDomainEvents().register(DomainEvent.delete(Topics.JOIN_GAME_TOPIC.name(), this.id.getId()));
            GameEvents.getDomainEvents().register(DomainEvent.delete(Topics.PLAYER_ACTION_TOPIC.name(), this.id.getId()));
        }

        @Override
        public void left() {
            scoreBoard.resetScore(this.id);
            GameEvents.getClientEvents().register(
                    newDtoEvent(ev -> ev.setPlayerLeft(PLAYER_LEFT_BUILDER
                            .clear()
                            .setPlayerId(id.getGuid())))
            );
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
            GameEvents.getDomainEvents().register(
                    DomainEvent.create(
                            Topics.PLAYER_ACTION_TOPIC.name(),
                            this.id.getId(),
                            DtoUtils
                                    .newEvent()
                                    .setPlayerDestroyed(PLAYER_DESTROYED_BUILDER
                                            .clear()
                                            .setPlayerId(this.id.getGuid())
                                            .setDestroyedByPlayerId(byPlayerId.getGuid())
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
            GameEvents.getClientEvents().register(
                    newDtoEvent(ev -> ev.setPlayerDestroyed(event))
            );
        }

        @Override
        public void boltExhausted() {
            this.liveBolts = Math.max(0, liveBolts - 1);
        }

        @Override
        public void spawn() {
            if (!this.spawned) {
                setSpawned(true);
                long now = clock.getTime();
                resetPosition(now);
                GameEvents.getDomainEvents().register(
                        DomainEvent.create(Topics.PLAYER_ACTION_TOPIC.name(),
                                this.id.getId(),
                                DtoUtils
                                        .newEvent()
                                        .setPlayerSpawned(
                                                PLAYER_SPAWNED_BUILDER
                                                        .clear()
                                                        .setLocation(
                                                                PLAYER_MOVED_BUILDER
                                                                        .clear()
                                                                        .setPlayerId(id.getGuid())
                                                                        .setX(position.getX())
                                                                        .setY(position.getY())
                                                                        .setAngle(angle)
                                                                        .setThrustAngle(position.getVector().getAngle())
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
            if (event.getLocation().getTimestamp() > this.movedTimestamp) {
                setSpawned(true);
                this.position.initialized(event.getLocation().getX(),
                        event.getLocation().getY(),
                        0L);
                movedOrSpawned(event.getLocation(), event);
            }
        }

        @Override
        @IsThreadSafe
        public void expungeIfStalled() {
            if (!spawned && clock.getTime() - this.lastSpawnOrUnspawnTimestamp > Config.get().getExpungeSinceLastSpawnTimeout()) {
                leave();
            }
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
            public boolean isBounded() {
                return true;
            }

            @Override
            public OptionalDouble maxMagnitude() {
                return OptionalDouble.of(Config.get().getPlayerMaxMove());
            }
        };
    }
}
