package me.pcasaes.bbop.model;

import me.pcasaes.bbop.dto.EventType;
import me.pcasaes.bbop.model.annotations.IsThreadSafe;

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

    static Bolts create() {
        return new Bolts();
    }

    private final Map<UUID, Bolt> activeBolts = new HashMap<>();


    Optional<Bolt> fired(
            Players players,
            UUID boltId,
            UUID ownerPlayerId,
            float x,
            float y,
            float angle,
            long startTimestamp) {
        if (activeBolts.containsKey(boltId)) {
            return Optional.empty();
        }
        Bolt bolt = Bolt.create(players, boltId, ownerPlayerId, x, y, angle, startTimestamp);
        activeBolts.put(bolt.getId(), bolt);
        return Optional.of(bolt);
    }


    public void fixedUpdate(final long timestamp) {
        activeBolts
                .values()
                .stream()
                .map(b -> b.updateTimestamp(timestamp))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Bolt::tackleBoltExhaustion)
                .filter(Bolt::isActive)
                .map(Bolt::move)
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

    @IsThreadSafe
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
}