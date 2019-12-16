package me.paulo.casaes.bbop.model;

import me.paulo.casaes.bbop.dto.BoltExhaustedEventDto;
import me.paulo.casaes.bbop.dto.BoltMovedEventDto;
import me.paulo.casaes.bbop.dto.EventDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class Bolt {

    private static final Map<UUID, Bolt> ACTIVE_BOLTS = new HashMap<>();

    private UUID id;
    private String idString;
    private String ownerPlayerId;
    private float prevX;
    private float x;
    private float prevY;
    private float y;
    private float angle;
    private float speed;
    private long timestamp;
    private long startTimestamp;
    private boolean exhausted;

    private Bolt(String ownerPlayerId, float x, float y, float angle, float speed) {
        this.id = UUID.randomUUID();
        this.idString = this.id.toString();
        this.ownerPlayerId = ownerPlayerId;
        this.x = x;
        this.y = y;
        this.prevX = x;
        this.prevY = y;
        this.angle = angle;
        this.speed = speed;
        this.timestamp = Clock.get().getTime();
        this.startTimestamp = this.timestamp;
        this.exhausted = false;
    }

    static Bolt fire(String ownerPlayerId, float x, float y, float angle, float speedAdjustment) {
        Bolt bolt = new Bolt(ownerPlayerId, x, y, angle, Config.get().getBoltSpeed() + speedAdjustment);
        ACTIVE_BOLTS.put(bolt.id, bolt);
        return bolt;
    }

    boolean is(UUID id) {
        return this.id.equals(id);
    }

    private void move(long timestamp) {
        long elapsed = Math.max(0L, timestamp - this.timestamp);
        this.timestamp = timestamp;

        EventDto event = null;
        this.prevX = this.x;
        this.prevY = this.y;
        if (elapsed > 0L) {
            float r = speed * elapsed / 1000f;

            float ox = this.x;
            float oy = this.y;

            float mx = (float) Math.cos(angle) * r;
            float my = (float) Math.sin(angle) * r;

            float minMove = Config.get().getMinMove();
            if (Math.abs(mx) > minMove) {
                this.x += mx;
            }
            if (Math.abs(my) > minMove) {
                this.y += my;
            }

            if (ox != this.x || oy != this.y) {
                event = toEvent();
            }
        }

        if (event != null) {
            GameEvents.get().register(event);
        }
    }

    boolean isExhausted() {
        if (this.exhausted) {
            return true;
        }
        if (x < 0f || x > 1f ||
                y < 0f || y > 1f ||
                this.timestamp - this.startTimestamp > Config.get().getBoltMaxDuration()) {
            this.exhausted = true;
            return true;
        }
        return false;
    }

    boolean isActive() {
        return !isExhausted();
    }

    private void hits() {
        for (Player player : Players.get().iterable()) {
            hit(player);
        }
    }

    private void hit(Player player) {
        boolean isHit = !player.is(ownerPlayerId) && player.collision(prevX, prevY, x, y, Config.get().getBoltCollisionRadius());

        if (isHit) {
            player.destroyedBy(this.ownerPlayerId);
            if (!this.exhausted) {
                this.exhausted = true;
                GameEvents.get().register(BoltExhaustedEventDto.of(this.idString));
            }
        }
    }

    EventDto toEvent() {
        return isExhausted() ?
                BoltExhaustedEventDto.of(this.idString) :
                BoltMovedEventDto.of(this.idString, this.ownerPlayerId, this.x, this.y, this.angle);
    }

    public static void fixedUpdate(final long timestamp) {
        ACTIVE_BOLTS
                .values()
                .forEach(b -> b.move(timestamp));

        ACTIVE_BOLTS
                .values()
                .forEach(Bolt::hits);

        cleanup();
    }

    public static Iterable<Bolt> iterable() {
        return ACTIVE_BOLTS
                .values();
    }

    private static void cleanup() {
        List<UUID> toRemove = ACTIVE_BOLTS
                .entrySet()
                .stream()
                .filter(e -> e.getValue().isExhausted())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        toRemove
                .forEach(ACTIVE_BOLTS::remove);

    }

    boolean isOwnedBy(String playerId) {
        return this.ownerPlayerId.equals(playerId);
    }

    static void reset() {
        ACTIVE_BOLTS.clear();
    }

}
