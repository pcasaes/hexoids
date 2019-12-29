package me.paulo.casaes.bbop.model;

import me.paulo.casaes.bbop.dto.EventType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Bolts implements Iterable<Bolt> {

    private static final Bolts INSTANCE = new Bolts();

    private final Map<UUID, Bolt> activeBolts = new HashMap<>();


    public static Bolts get() {
        return INSTANCE;
    }

    Optional<Bolt> fired(
            UUID boltId,
            String ownerPlayerId,
            float x,
            float y,
            float angle,
            float speedAdjustment,
            long startTimestamp) {
        if (activeBolts.containsKey(boltId)) {
            return Optional.empty();
        }
        Bolt bolt = Bolt.create(boltId, ownerPlayerId, x, y, angle, speedAdjustment, startTimestamp);
        activeBolts.put(bolt.getId(), bolt);
        return Optional.of(bolt);
    }


    public void fixedUpdate(final long timestamp) {
        activeBolts
                .values()
                .forEach(b -> b.move(timestamp));

        activeBolts
                .values()
                .forEach(Bolt::checkHits);

        cleanup();
    }

    @Override
    public Iterator<Bolt> iterator() {
        return activeBolts
                .values()
                .iterator();
    }

    public Stream<Bolt> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    public void consumeFromBoltActionTopic(DomainEvent domainEvent) {
        if (domainEvent.getEvent() != null &&
                (domainEvent.getEvent().isEvent(EventType.BOLT_MOVED) || domainEvent.getEvent().isEvent(EventType.BOLT_EXHAUSTED))) {
            GameEvents.getClientEvents().register(domainEvent.getEvent());
        }
    }

    private void cleanup() {
        List<UUID> toRemove = activeBolts
                .entrySet()
                .stream()
                .filter(e -> e.getValue().isExhausted())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        toRemove
                .forEach(activeBolts::remove);

    }

    void reset() {
        activeBolts.clear();
    }
}
