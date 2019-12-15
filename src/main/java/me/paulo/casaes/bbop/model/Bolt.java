package me.paulo.casaes.bbop.model;

import me.paulo.casaes.bbop.dto.BoltExhaustedEventDto;
import me.paulo.casaes.bbop.dto.BoltMovedEventDto;
import me.paulo.casaes.bbop.dto.EventDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Bolt {

    private static final Map<UUID, Bolt> ACTIVE_BOLTS = new HashMap<>();

    private static final float COLLISION_RADIUS = 0.001f;

    private static final long MAX_DURATION = 10_000;

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
        this.timestamp = System.currentTimeMillis();
        this.startTimestamp = this.timestamp;
        this.exhausted = false;
    }

    static Bolt fire(String ownerPlayerId, float x, float y, float angle, float speedAdjustment) {
        Bolt bolt = new Bolt(ownerPlayerId, x, y, angle, 0.07f + speedAdjustment);
        ACTIVE_BOLTS.put(bolt.id, bolt);
        return bolt;
    }

    private Optional<EventDto> move(long timestamp) {
        long elapsed = Math.max(0L, timestamp - this.timestamp);

        EventDto event = null;
        this.prevX = this.x;
        this.prevY = this.y;
        if (elapsed > 0L) {
            float r = speed * elapsed / 1000f;

            float mx = (float) Math.cos(angle) * r;
            float my = (float) Math.sin(angle) * r;

            this.x += mx;
            this.y += my;

            event = toEvent();
        }
        this.timestamp = timestamp;

        return Optional.ofNullable(event);
    }

    boolean isExhausted() {
        if (this.exhausted) {
            return true;
        }
        if (x < 0f || x > 1f ||
                y < 0f || y > 1f ||
                this.timestamp - this.startTimestamp > MAX_DURATION) {
            this.exhausted = true;
            return true;
        }
        return false;
    }

    private List<EventDto> hits() {
        return StreamSupport
                .stream(Player.iterable().spliterator(), false)
                .map(this::hit)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<EventDto> hit(Player player) {
        if (isExhausted()) {
            return Collections.emptyList();
        }
        boolean isHit = !player.getId().equals(ownerPlayerId) && player.collision(prevX, prevY, x, y, COLLISION_RADIUS);

        if (isHit) {
            this.exhausted = true;
            List<EventDto> events = new ArrayList<>(2);
            events.add(player.destroyedBy(this.ownerPlayerId));
            events.add(BoltExhaustedEventDto.of(this.idString));
            return events;
        }

        return Collections.emptyList();
    }

    private EventDto toEvent() {
        return isExhausted() ?
                BoltExhaustedEventDto.of(this.idString) :
                BoltMovedEventDto.of(this.idString, this.ownerPlayerId, this.x, this.y, this.angle);
    }

    public static List<EventDto> update(final long timestamp) {
        List<EventDto> events = new ArrayList<>();

        ACTIVE_BOLTS
                .values()
                .stream()
                .map(b -> b.move(timestamp))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(events::add);

        ACTIVE_BOLTS
                .values()
                .stream()
                .map(Bolt::hits)
                .flatMap(List::stream)
                .forEach(events::add);

        cleanup();
        return events;
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

}
