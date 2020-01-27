package me.pcasaes.bbop.model;

import me.pcasaes.bbop.model.annotations.IsThreadSafe;
import me.pcasaes.bbop.util.TrigUtil;
import pcasaes.bbop.proto.BoltFiredEventDto;
import pcasaes.bbop.proto.PlayerDestroyedEventDto;
import pcasaes.bbop.proto.PlayerDto;
import pcasaes.bbop.proto.PlayerJoinedEventDto;
import pcasaes.bbop.proto.PlayerMovedEventDto;
import pcasaes.bbop.proto.PlayerSpawnedEventDto;

import java.util.Objects;
import java.util.Optional;
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

    void move(float moveX, float moveY, Float angle, Float thrustAngle);

    void moved(PlayerMovedEventDto event);

    void leave();

    void left();

    boolean collision(VelocityVector velocityVector, float collisionRadius);

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

        private float x = 0f;

        private float y = 0f;

        private float angle = 0f;

        private float thrustAngle = 0f;

        private long movedTimestamp;

        private int liveBolts = 0;

        private final Players players;

        private final Bolts bolts;

        private final Clock clock;

        private final ScoreBoard scoreBoard;

        private final ResetPosition resetPosition;

        private Implementation(EntityId id, Players players, Bolts bolts, Clock clock, ScoreBoard scoreBoard) {
            this.players = players;
            this.bolts = bolts;
            this.clock = clock;
            this.scoreBoard = scoreBoard;
            this.id = id;

            this.ship = RNG.nextInt(6);
            setSpawned(false);
            this.resetPosition = ResetPosition.create(Config.get().getPlayerResetPosition());
        }

        private void setSpawned(boolean spawned) {
            this.spawned = spawned;
            this.lastSpawnOrUnspawnTimestamp = clock.getTime();
        }

        @Override
        public void fire() {
            if (!spawned) {
                return;
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
                                            .setX(x)
                                            .setY(y)
                                            .setAngle(angle)
                                            .setStartTimestamp(clock.getTime())
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
                    .setX(x)
                    .setY(y)
                    .setAngle(angle)
                    .setSpawned(spawned)
                    .build();
        }

        private void resetPosition() {
            this.x = resetPosition.getNextX();
            this.y = resetPosition.getNextY();
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
        public void move(float moveX, float moveY, Float angle, Float thrustAngle) {
            if (!this.spawned) {
                return;
            }

            float minMove = Config.get().getMinMove();
            if (angle == null &&
                    Math.abs(moveX) <= minMove &&
                    Math.abs(moveY) <= minMove
            ) {
                return;
            }

            float maxMove = Config.get().getPlayerMaxMove();
            moveX = Math.max(-maxMove, Math.min(moveX, maxMove));
            moveY = Math.max(-maxMove, Math.min(moveY, maxMove));

            float nx = this.x + moveX;
            float ny = this.y + moveY;
            if (angle != null) {
                this.angle = TrigUtil.limitRotation(this.angle, angle, Config.get().getPlayerMaxAngle());
            }

            this.x = Math.max(0f, Math.min(1f, nx));
            this.y = Math.max(0f, Math.min(1f, ny));

            if (thrustAngle != null) {
                this.thrustAngle = thrustAngle;
            }

            fireMoveDomainEvent();
        }

        @Override
        public void moved(PlayerMovedEventDto event) {
            movedOrSpawned(event, null);
        }

        private void movedOrSpawned(PlayerMovedEventDto movedEvent, PlayerSpawnedEventDto spawnedEvent) {
            if (movedEvent.getTimestamp() > this.movedTimestamp) {
                this.x = movedEvent.getX();
                this.y = movedEvent.getY();
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

        private void fireMoveDomainEvent() {
            GameEvents.getDomainEvents().register(
                    DomainEvent.create(Topics.PLAYER_ACTION_TOPIC.name(),
                            this.id.getId(),
                            DtoUtils
                                    .newEvent()
                                    .setPlayerMoved(PLAYER_MOVED_BUILDER
                                            .clear()
                                            .setPlayerId(id.getGuid())
                                            .setX(x)
                                            .setY(y)
                                            .setAngle(angle)
                                            .setThrustAngle(thrustAngle)
                                            .setTimestamp(clock.getTime())
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
        public boolean collision(VelocityVector velocityVector, float collisionRadius) {
            if (!this.spawned) {
                return false;
            }
            return velocityVector.intersectedWith(this.x, this.y, collisionRadius);
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
                resetPosition();
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
                                                                        .setX(x)
                                                                        .setY(y)
                                                                        .setAngle(angle)
                                                                        .setThrustAngle(thrustAngle)
                                                                        .setTimestamp(clock.getTime())
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
    }
}
