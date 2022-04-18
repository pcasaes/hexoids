package me.pcasaes.hexoids.core.domain.model;

import pcasaes.hexoids.proto.BoltDivertedEventDto;
import pcasaes.hexoids.proto.BoltFiredEventDto;
import pcasaes.hexoids.proto.DirectedCommand;
import pcasaes.hexoids.proto.Dto;
import pcasaes.hexoids.proto.GUID;
import pcasaes.hexoids.proto.LiveBoltListCommandDto;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The collection of bolts. This collections only tracks bolts maintained
 * by this node.
 */
public class Bolts implements Iterable<Bolt> {

    /**
     * Creates the collection.
     *
     * @return
     */
    static Bolts create() {
        return new Bolts();
    }

    private final Map<EntityId, Bolt> activeBolts = new HashMap<>();
    private final Map<GUID, BoltFiredEventDto> publishableBoltDtos = new HashMap<>();


    /**
     * Processes a bolt fired event.
     *
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
            long startTimestamp,
            int ttl) {
        if (activeBolts.containsKey(boltId)) {
            return Optional.empty();
        }
        Bolt bolt = Bolt.create(players, boltId, ownerPlayerId, x, y, angle, speed, startTimestamp, ttl);
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
                .map(b -> b.tackleBoltExhaustion(timestamp))
                .filter(Bolt::isActive)
                .forEach(b -> b.checkHits(timestamp));

        cleanup();
    }

    /**
     * Returns an iterator of the collection.
     *
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
     *
     * @return
     */
    public Stream<Bolt> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Handles bolt action domain events.
     *
     * @param domainEvent
     */
    public void consumeFromBoltActionTopic(DomainEvent domainEvent) {
        if (domainEvent.getEvent() != null &&
                (domainEvent.getEvent().hasBoltExhausted() ||
                        domainEvent.getEvent().hasBoltFired() ||
                        domainEvent.getEvent().hasBoltDiverted())) {

            GameEvents.getClientEvents()
                    .dispatch(Dto.newBuilder()
                            .setEvent(domainEvent.getEvent())
                            .build());

            if (domainEvent.getEvent().hasBoltFired()) {
                publishableBoltDtos.put(
                        domainEvent.getEvent().getBoltFired().getBoltId(),
                        domainEvent.getEvent().getBoltFired());

            } else if (domainEvent.getEvent().hasBoltExhausted()) {
                publishableBoltDtos.remove(domainEvent.getEvent().getBoltExhausted().getBoltId());
            } else if (domainEvent.getEvent().hasBoltDiverted()) {
                consumerBoltDiverted(domainEvent.getEvent().getBoltDiverted());
            }
        }
    }

    private void consumerBoltDiverted(BoltDivertedEventDto boltDiverted) {
        GUID boltId = boltDiverted.getBoltId();
        BoltFiredEventDto boltFiredEventDto = publishableBoltDtos.get(boltId);
        if (boltFiredEventDto != null) {
            int newTtl = boltFiredEventDto.getTtl() - (int) (boltDiverted.getDivertTimestamp() - boltFiredEventDto.getStartTimestamp());
            if (newTtl > 0) {
                publishableBoltDtos.put(
                        boltId,
                        BoltFiredEventDto.newBuilder()
                                .mergeFrom(boltFiredEventDto)
                                .setX(boltDiverted.getX())
                                .setY(boltDiverted.getY())
                                .setAngle(boltDiverted.getAngle())
                                .setSpeed(boltDiverted.getSpeed())
                                .setStartTimestamp(boltDiverted.getDivertTimestamp())
                                .setTtl(newTtl)
                                .build());
            } else {
                publishableBoltDtos.remove(boltId);
            }
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
                .stream()
                .map(activeBolts::remove)
                .forEach(Bolt::destroyObject);
    }

    public void requestListOfLiveBolts(EntityId requesterId) {
        LiveBoltListCommandDto.Builder liveBoltsList = LiveBoltListCommandDto.newBuilder()
                .addAllBolts(publishableBoltDtos.values());


        DirectedCommand.Builder builder = DirectedCommand.newBuilder()
                .setPlayerId(requesterId.getGuid())
                .setLiveBoltsList(liveBoltsList);

        GameEvents.getClientEvents().dispatch(
                Dto.newBuilder()
                        .setDirectedCommand(builder)
                        .build()
        );
    }

    /**
     * Return the total number of active bolts in the game.
     * Is weakly consistent and thread safe.
     *
     * @return
     */
    public int getTotalNumberOfActiveBolts() {
        return this.activeBolts.size();
    }
}
