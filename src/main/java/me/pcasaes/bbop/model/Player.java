package me.pcasaes.bbop.model;

import me.pcasaes.bbop.dto.BoltFiredEventDto;
import me.pcasaes.bbop.dto.PlayerDestroyedEventDto;
import me.pcasaes.bbop.dto.PlayerDto;
import me.pcasaes.bbop.dto.PlayerJoinedEventDto;
import me.pcasaes.bbop.dto.PlayerLeftEventDto;
import me.pcasaes.bbop.dto.PlayerMovedOrSpawnedEventDto;
import me.pcasaes.bbop.model.annotations.IsThreadSafe;
import me.pcasaes.bbop.util.TrigUtil;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public interface Player {

    static Player create(UUID id, Players players, Bolts bolts, Clock clock, ScoreBoard scoreBoard) {
        return new Implementation(id, players, bolts, clock, scoreBoard);
    }

    void fire();

    void fired(BoltFiredEventDto event);

    int getActiveBoltCount();

    boolean is(UUID playerId);

    PlayerDto toDto();

    void join();

    void joined(PlayerJoinedEventDto event);

    void move(float moveX, float moveY, Float angle, Float thrustAngle);

    void moved(PlayerMovedOrSpawnedEventDto event);

    void leave();

    void left();

    boolean collision(float x1, float y1, float x2, float y2, float collisionRadius);

    void destroy(UUID playerId);

    void destroyed(PlayerDestroyedEventDto event);

    void boltExhausted();

    void spawn();

    void spawned(PlayerMovedOrSpawnedEventDto event);

    void expungeIfStalled();

    class Implementation implements Player {

        private static final Random RNG = new Random();

        private final UUID id;

        private final String idStr;

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

        private Implementation(UUID id, Players players, Bolts bolts, Clock clock, ScoreBoard scoreBoard) {
            this.players = players;
            this.bolts = bolts;
            this.clock = clock;
            this.scoreBoard = scoreBoard;
            this.id = id;
            this.idStr = id.toString();

            this.ship = RNG.nextInt(6);
            setSpawned(false);
            this.resetPosition = ResetPosition.create(Config.get().getPlayerResetPosition());
        }

        private void setSpawned(boolean spawned) {
            this.spawned = spawned;
            this.lastSpawnOrUnspawnTimestamp = clock.getTime();
        }

        @Override
        @IsThreadSafe
        public void fire() {
            if (!spawned) {
                return;
            }
            final UUID boltId = UUID.randomUUID();
            GameEvents.getDomainEvents()
                    .register(DomainEvent.create(
                            Topics.BOLT_LIFECYCLE_TOPIC.name(),
                            boltId,
                            BoltFiredEventDto.of(
                                    boltId,
                                    this.id,
                                    this.x,
                                    this.y,
                                    this.angle,
                                    this.clock.getTime()
                            )
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
                    event.getBoltId(),
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
        public boolean is(UUID playerId) {
            return id.equals(playerId);
        }

        @Override
        public PlayerDto toDto() {
            return PlayerDto.of(idStr, ship, x, y, angle);
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
                                    this.id,
                                    PlayerJoinedEventDto.of(idStr, ship))
            );
        }

        @Override
        public void joined(PlayerJoinedEventDto event) {
            this.ship = event.getShip();
            GameEvents.getClientEvents().register(event);
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
        public void moved(PlayerMovedOrSpawnedEventDto event) {
            if (event.getTimestamp() > this.movedTimestamp) {
                this.x = event.getX();
                this.y = event.getY();
                this.angle = event.getAngle();
                this.movedTimestamp = event.getTimestamp();
                GameEvents.getClientEvents().register(event);
            }
        }

        private void fireMoveDomainEvent() {
            GameEvents.getDomainEvents().register(
                    DomainEvent.create(Topics.PLAYER_ACTION_TOPIC.name(),
                            this.id,
                            PlayerMovedOrSpawnedEventDto.moved(
                                    this.idStr,
                                    this.x,
                                    this.y,
                                    this.angle,
                                    this.thrustAngle,
                                    this.clock.getTime())));
        }

        @Override
        @IsThreadSafe
        public void leave() {
            GameEvents.getDomainEvents().register(DomainEvent.delete(Topics.JOIN_GAME_TOPIC.name(), this.id));
            GameEvents.getDomainEvents().register(DomainEvent.delete(Topics.PLAYER_ACTION_TOPIC.name(), this.id));
        }

        @Override
        public void left() {
            scoreBoard.resetScore(this.id);
            GameEvents.getClientEvents().register(PlayerLeftEventDto.of(this.idStr));
        }

        @Override
        public boolean collision(float x1, float y1, float x2, float y2, float collisionRadius) {
            if (!this.spawned) {
                return false;
            }
            float minx = Math.min(x1, x2);
            float maxx = Math.max(x1, x2);
            if (this.x - collisionRadius > maxx || this.x + collisionRadius < minx) {
                return false;
            }

            float miny = Math.min(y1, y2);
            float maxy = Math.max(y1, y2);
            if (this.y - collisionRadius > maxy || this.y + collisionRadius < miny) {
                return false;
            }

            //https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line
            float distance = TrigUtil.calculateShortestDistanceFromPointToLine(x1, y1, x2, y2, this.x, this.y);
            return distance <= collisionRadius;

        }

        @Override
        public void destroy(UUID byPlayerId) {
            GameEvents.getDomainEvents().register(
                    DomainEvent.create(
                            Topics.PLAYER_ACTION_TOPIC.name(),
                            this.id,
                            PlayerDestroyedEventDto.of(this.id, byPlayerId))
            );
            setSpawned(false);
            this.scoreBoard.updateScore(byPlayerId, 1);
        }

        @Override
        public void destroyed(PlayerDestroyedEventDto event) {
            this.scoreBoard.resetScore(event.getPlayerId());
            GameEvents.getClientEvents().register(event);
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
                                this.id,
                                PlayerMovedOrSpawnedEventDto.spawned(
                                        this.idStr,
                                        this.x,
                                        this.y,
                                        this.angle,
                                        this.thrustAngle,
                                        this.clock.getTime())));
            }
        }

        @Override
        public void spawned(PlayerMovedOrSpawnedEventDto event) {
            if (event.getTimestamp() > this.movedTimestamp) {
                setSpawned(true);
                moved(event);
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