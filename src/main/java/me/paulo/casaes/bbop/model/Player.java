package me.paulo.casaes.bbop.model;

import me.paulo.casaes.bbop.dto.PlayerDestroyedEventDto;
import me.paulo.casaes.bbop.dto.PlayerDto;
import me.paulo.casaes.bbop.dto.PlayerJoinedEventDto;
import me.paulo.casaes.bbop.dto.PlayerLeftEventDto;
import me.paulo.casaes.bbop.dto.PlayerMovedEventDto;
import me.paulo.casaes.bbop.util.TrigUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.stream.Collectors;

public interface Player {

    static Player create(String id) {
        return new Implementation(id);
    }

    void fire();

    Iterable<Bolt> getActiveBolts();

    boolean is(String playerId);

    PlayerDto toDto();

    void join();

    void joined(PlayerJoinedEventDto event);

    void move(float moveX, float moveY, Float angle);

    void moved(PlayerMovedEventDto event);

    void leave();

    void left();

    boolean collision(float x1, float y1, float x2, float y2, float collisionRadius);

    void destroyedBy(String playerId);

    class Implementation implements Player {

        private static final Random RNG = new Random();

        private final String id;

        private int ship;

        private float x = 0f;

        private float y = 0f;

        private float angle = 0f;

        private float currentSpeed = 0f;

        private long movedTimestamp;

        private Collection<Bolt> firedBolts = new ArrayList<>();

        private Implementation(String id) {
            this.id = id;

            this.ship = RNG.nextInt(6);
        }

        @Override
        public void fire() {
            firedBolts.removeIf(Bolt::isExhausted);
            if (firedBolts.size() < Config.get().getMaxBolts()) {
                firedBolts.add(Bolt.fire(this.id, this.x, this.y, this.angle, this.currentSpeed));
            }
        }

        @Override
        public Iterable<Bolt> getActiveBolts() {
            return this.firedBolts
                    .stream()
                    .filter(Bolt::isActive)
                    .collect(Collectors.toList());
        }

        @Override
        public boolean is(String playerId) {
            return id.equals(playerId);
        }

        @Override
        public PlayerDto toDto() {
            return PlayerDto.of(id, ship, x, y, angle);
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
                                    PlayerJoinedEventDto.of(id, ship, x, y, angle))
            );
        }

        @Override
        public void joined(PlayerJoinedEventDto event) {
            this.x = event.getX();
            this.y = event.getY();
            this.angle = event.getAngle();
            this.ship = event.getShip();
            GameEvents.getClientEvents().register(PlayerJoinedEventDto.of(id, ship, x, y, angle));
        }

        @Override
        public void move(float moveX, float moveY, Float angle) {

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
                                    this.id,
                                    this.x,
                                    this.y,
                                    this.angle,
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
            GameEvents.getClientEvents().register(PlayerLeftEventDto.of(this.id));
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
        public void destroyedBy(String playerId) {
            resetPosition();

            GameEvents.getDomainEvents().register(
                    DomainEvent.create(
                            Topics.PlayerActionTopic.name(),
                            this.id,
                            PlayerDestroyedEventDto.of(this.id, playerId))
            );
            fireMoveDomainEvent();
            ScoreBoard.Factory.get().updateScore(playerId, 1);
            ScoreBoard.Factory.get().resetScore(this.id);
        }
    }
}
