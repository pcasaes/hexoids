package me.paulo.casaes.bbop.model;

import me.paulo.casaes.bbop.dto.BoltFiredEventDto;
import me.paulo.casaes.bbop.dto.PlayerDestroyedEventDto;
import me.paulo.casaes.bbop.dto.PlayerDto;
import me.paulo.casaes.bbop.dto.PlayerJoinedEventDto;
import me.paulo.casaes.bbop.dto.PlayerLeftEventDto;
import me.paulo.casaes.bbop.dto.PlayerMovedEventDto;
import me.paulo.casaes.bbop.model.annotations.IsThreadSafe;
import me.paulo.casaes.bbop.util.TrigUtil;

import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public interface Player {

    static Player create(UUID id) {
        return new Implementation(id);
    }

    void fire();

    void fired(BoltFiredEventDto event);

    int getActiveBoltCount();

    boolean is(UUID playerId);

    PlayerDto toDto();

    void join();

    void joined(PlayerJoinedEventDto event);

    void move(float moveX, float moveY, Float angle, Float thrustAngle);

    void moved(PlayerMovedEventDto event);

    void leave();

    void left();

    boolean collision(float x1, float y1, float x2, float y2, float collisionRadius);

    void destroyedBy(UUID playerId);

    void destroyed(PlayerDestroyedEventDto event);

    void boltExhausted();

    class Implementation implements Player {

        private static final Random RNG = new Random();

        private final UUID id;

        private final String idStr;

        private int ship;

        private float x = 0f;

        private float y = 0f;

        private float angle = 0f;

        private float thrustAngle = 0f;

        private float currentSpeed = 0f;

        private long movedTimestamp;

        private int liveBolts = 0;

        private Implementation(UUID id) {
            this.id = id;
            this.idStr = id.toString();

            this.ship = RNG.nextInt(6);
        }

        @Override
        @IsThreadSafe
        public void fire() {
            GameEvents.getDomainEvents()
                    .register(DomainEvent.create(
                            Topics.BoltLifecycleTopic.name(),
                            this.id,
                            BoltFiredEventDto.of(
                                    UUID.randomUUID().toString(),
                                    this.idStr,
                                    this.x,
                                    this.y,
                                    this.angle,
                                    this.currentSpeed,
                                    Clock.Factory.get().getTime()
                            )
                    ));
        }

        public void fired(BoltFiredEventDto event) {
            if (this.liveBolts < Config.get().getMaxBolts()) {
                Bolts.get().fired(
                        UUID.fromString(event.getBoltId()),
                        this.id,
                        event.getX(),
                        event.getY(),
                        event.getAngle(),
                        event.getSpeedAdjustment(),
                        event.getStartTimestamp())
                        .ifPresent(b -> this.liveBolts++);
            }
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
            if (Config.get().getEnv() == Config.Environment.DEV) {
                this.x = 0f;
                this.y = 0f;
            } else {
                this.x = RNG.nextFloat();
                this.y = RNG.nextFloat();
            }
            this.angle = 0f;
        }

        @Override
        public void join() {
            resetPosition();
            GameEvents.getDomainEvents().register(
                    DomainEvent
                            .create(Topics.JoinGameTopic.name(),
                                    this.id,
                                    PlayerJoinedEventDto.of(idStr, ship, x, y, angle))
            );
        }

        @Override
        public void joined(PlayerJoinedEventDto event) {
            this.x = event.getX();
            this.y = event.getY();
            this.angle = event.getAngle();
            this.ship = event.getShip();
            GameEvents.getClientEvents().register(PlayerJoinedEventDto.of(idStr, ship, x, y, angle));
        }

        @Override
        public void move(float moveX, float moveY, Float angle, Float thrustAngle) {

            float minMove = Config.get().getMinMove();
            if (Math.abs(moveX) <= minMove &&
                    Math.abs(moveY) <= minMove
            ) {
                this.currentSpeed = 0f;
                if (angle == null) {
                    return;
                }
            }

            float maxMove = Config.get().getPlayerMaxMove();
            moveX = Math.max(-maxMove, Math.min(moveX, maxMove));
            moveY = Math.max(-maxMove, Math.min(moveY, maxMove));

            float nx = this.x + moveX;
            float ny = this.y + moveY;
            if (angle != null) {

                final float maxAngle = Config.get().getPlayerMaxAngle();
                float aDiff1 = TrigUtil.calculateAngleDistance(angle, this.angle);
                if (aDiff1 > maxAngle) {
                    aDiff1 = maxAngle;
                    this.angle += aDiff1;
                } else if (aDiff1 < -maxAngle) {
                    aDiff1 = -maxAngle;
                    this.angle += aDiff1;
                } else {
                    this.angle = angle;
                }
            }

            this.currentSpeed = (float) Math.sqrt(Math.pow(Math.abs(moveX), 2) + Math.pow(Math.abs(moveY), 2));


            this.x = Math.max(0f, Math.min(1f, nx));
            this.y = Math.max(0f, Math.min(1f, ny));

            if (thrustAngle != null) {
                this.thrustAngle = thrustAngle;
            }

            fireMoveDomainEvent();
        }

        @Override
        public void moved(PlayerMovedEventDto event) {
            if (event.getTimestamp() > this.movedTimestamp) {
                this.x = event.getX();
                this.y = event.getY();
                this.angle = event.getAngle();
                this.movedTimestamp = event.getTimestamp();
            }
            GameEvents.getClientEvents().register(event);
        }

        private void fireMoveDomainEvent() {
            this.movedTimestamp = Clock.Factory.get().getTime();
            GameEvents.getDomainEvents().register(
                    DomainEvent.create(Topics.PlayerActionTopic.name(),
                            this.id,
                            PlayerMovedEventDto.of(
                                    this.idStr,
                                    this.x,
                                    this.y,
                                    this.angle,
                                    this.thrustAngle,
                                    this.currentSpeed,
                                    this.movedTimestamp)));
        }

        @Override
        public void leave() {
            GameEvents.getDomainEvents().register(DomainEvent.delete(Topics.JoinGameTopic.name(), this.id));
            GameEvents.getDomainEvents().register(DomainEvent.delete(Topics.PlayerActionTopic.name(), this.id));
        }

        @Override
        public void left() {
            GameEvents.getClientEvents().register(PlayerLeftEventDto.of(this.idStr));
        }

        @Override
        public boolean collision(float x1, float y1, float x2, float y2, float collisionRadius) {
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
        public void destroyedBy(UUID playerId) {
            resetPosition();

            GameEvents.getDomainEvents().register(
                    DomainEvent.create(
                            Topics.PlayerActionTopic.name(),
                            this.id,
                            PlayerDestroyedEventDto.of(this.idStr, playerId.toString()))
            );
            fireMoveDomainEvent();
            ScoreBoard.Factory.get().updateScore(playerId, 1);
            ScoreBoard.Factory.get().resetScore(this.id);
        }

        @Override
        public void destroyed(PlayerDestroyedEventDto event) {
            GameEvents.getClientEvents().register(event);
        }

        @Override
        public void boltExhausted() {
            this.liveBolts = Math.max(0, liveBolts - 1);
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
