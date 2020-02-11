package me.pcasaes.hexoids.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static me.pcasaes.hexoids.model.DtoUtils.DTO_BUILDER;

/**
 * The collection of bolts. This collections only tracks bolts maintained
 * by this node.
 */
public class Bolts implements Iterable<Bolt> {

    /**
     * Creates the collection.
     * @return
     */
    static Bolts create() {
        return new Bolts();
    }

    private final Map<EntityId, Bolt> activeBolts = new HashMap<>();


    /**
     * Processes a bolt fired event.
     * @param players
     * @param boltId
     * @param ownerPlayerId
     * @param x
     * @param y
     * @param angle
     * @param speed
     * @param startTimestamp
     * @return
     */
    Optional<Bolt> fired(
            Players players,
            EntityId boltId,
            EntityId ownerPlayerId,
            float x,
            float y,
            float angle,
            float speed,
            long startTimestamp) {
        if (activeBolts.containsKey(boltId)) {
            return Optional.empty();
        }
        Bolt bolt = Bolt.create(players, boltId, ownerPlayerId, x, y, angle, speed, startTimestamp);
        activeBolts.put(bolt.getId(), bolt);
        return Optional.of(bolt);
    }


    /**
     * Updates all tracked bolts updating their position vectors. Checks if they hit and are exhausted.
     *
     * @param timestamp the timestamp to update the players to.
     */
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

    /**
     * Returns an iterator of the collection.
     * @return
     */
    @Override
    public Iterator<Bolt> iterator() {
        return activeBolts
                .values()
                .iterator();
    }

    /**
     * Returns a stream of the collection.
     * @return
     */
    public Stream<Bolt> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Handles bolt action domain events.
     * @param domainEvent
     */
    public void consumeFromBoltActionTopic(DomainEvent domainEvent) {
        if (domainEvent.getEvent() != null &&
                (domainEvent.getEvent().hasBoltMoved() || domainEvent.getEvent().hasBoltExhausted())) {
            GameEvents.getClientEvents()
                    .register(DTO_BUILDER
                            .clear()
                            .setEvent(domainEvent.getEvent())
                            .build());
        }
    }

    private void cleanup() {
        List<EntityId> toRemove = activeBolts
                .entrySet()
                .stream()
                .filter(e -> e.getValue().isExhausted())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        toRemove
                .forEach(activeBolts::remove);

    }
}
