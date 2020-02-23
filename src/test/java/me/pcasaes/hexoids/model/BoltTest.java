package me.pcasaes.hexoids.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pcasaes.hexoids.proto.BoltExhaustedEventDto;
import pcasaes.hexoids.proto.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BoltTest {

    @Mock
    private Clock clock;

    @Mock
    private Players players;

    private Bolts bolts;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        GameEvents.getClientEvents().setConsumer(null);
        GameEvents.getDomainEvents().setConsumer(null);

        bolts = Bolts.create();

        when(clock.getTime()).thenReturn(0L);

        Config.get().setBoltMaxDuration(10_000);
        Config.get().setBoltSpeed(0.01f);
        Config.get().setBoltCollisionRadius(0.001f);
        Config.get().setMinMove(0.000000001f);

        doAnswer(c -> Collections.emptyIterator()).when(players).iterator();
        doAnswer(c -> Collections.emptyList().spliterator()).when(players).spliterator();
        doAnswer(c -> Stream.empty()).when(players).stream();
    }

    @Test
    void testFireMoveAndExhaust() {
        List<DomainEvent> events = new ArrayList<>();
        GameEvents.getDomainEvents().setConsumer(events::add);

        EntityId one = EntityId.newId();
        Config.get().setBoltMaxDuration(1_500);
        final Bolt bolt = bolts.fired(players,
                EntityId.newId(),
                one,
                0f,
                0f,
                0f,
                Config.get().getBoltSpeed(),
                clock.getTime()).orElse(null);
        EntityId boltId = bolt.getId();
        assertNotNull(bolt);

        assertTrue(bolt.isOwnedBy(one));

        assertEquals(1,
                bolts
                        .stream()
                        .count());

        assertTrue(bolts
                .stream()
                .anyMatch(b -> bolt == b));

        assertFalse(bolt.isExhausted());
        assertTrue(bolt.isActive());

        when(clock.getTime()).thenReturn(1_000L);

        bolts.fixedUpdate(clock.getTime());

        assertFalse(bolt.isExhausted());
        assertTrue(bolt.isActive());

        when(clock.getTime()).thenReturn(2_000L);

        events.clear();
        bolts.fixedUpdate(clock.getTime());
        assertEquals(1, events.size());
        DomainEvent domainEvent = events.get(0);
        assertNotNull(domainEvent);

        assertTrue(domainEvent.getEvent().hasBoltExhausted());
        BoltExhaustedEventDto exhaustedEvent = domainEvent.getEvent().getBoltExhausted();

        assertNotNull(exhaustedEvent);
        assertEquals(boltId.getGuid().getGuid(), exhaustedEvent.getBoltId().getGuid());

        assertTrue(bolt.isExhausted());
        assertFalse(bolt.isActive());

        events.clear();

        assertEquals(0, bolts
                .stream()
                .count());
    }

    @Test
    void testMoveRight() {
        AtomicReference<DomainEvent> eventReference = new AtomicReference<>(null);
        GameEvents.getDomainEvents().setConsumer(eventReference::set);

        Config.get().setBoltMaxDuration(1_500);
        EntityId one = EntityId.newId();
        final Bolt bolt = bolts.fired(players,
                EntityId.newId(),
                one,
                0f,
                0f,
                0f,
                Config.get().getBoltSpeed(),
                clock.getTime()).orElse(null);
        assertNotNull(bolt);


        when(clock.getTime()).thenReturn(1_000L);

        bolts.fixedUpdate(clock.getTime());

        assertEquals(0.01f, bolt.getPositionVector().getX());
        assertEquals(0f, bolt.getPositionVector().getY());

    }

    @Test
    void testMoveRightDown() {
        AtomicReference<DomainEvent> eventReference = new AtomicReference<>(null);
        GameEvents.getDomainEvents().setConsumer(eventReference::set);

        Config.get().setBoltMaxDuration(1_500);
        EntityId one = EntityId.newId();
        final Bolt bolt = bolts.fired(players,
                EntityId.newId(),
                one,
                0f,
                0f,
                (float) Math.PI / 4f,
                Config.get().getBoltSpeed(),
                clock.getTime()).orElse(null);
        assertNotNull(bolt);


        when(clock.getTime()).thenReturn(1_000L);

        bolts.fixedUpdate(clock.getTime());

        assertEquals(0.0070710676f, bolt.getPositionVector().getX());
        assertEquals(0.0070710676f, bolt.getPositionVector().getY());
    }

    @Test
    void testMoveDown() {
        AtomicReference<DomainEvent> eventReference = new AtomicReference<>(null);
        GameEvents.getDomainEvents().setConsumer(eventReference::set);

        Config.get().setBoltMaxDuration(1_500);
        EntityId one = EntityId.newId();
        final Bolt bolt = bolts.fired(players,
                EntityId.newId(),
                one,
                0f,
                0f,
                (float) Math.PI / 2f,
                Config.get().getBoltSpeed(),
                clock.getTime()).orElse(null);
        assertNotNull(bolt);


        when(clock.getTime()).thenReturn(1_000L);

        bolts.fixedUpdate(clock.getTime());

        assertEquals(0f, bolt.getPositionVector().getX());
        assertEquals(0.01f, bolt.getPositionVector().getY());
    }

    @Test
    void testMoveLeftDown() {
        AtomicReference<DomainEvent> eventReference = new AtomicReference<>(null);
        GameEvents.getDomainEvents().setConsumer(eventReference::set);

        Config.get().setBoltMaxDuration(1_500);
        EntityId one = EntityId.newId();
        final Bolt bolt = bolts.fired(players,
                EntityId.newId(),
                one,
                1f,
                0f,
                (float) (3 * Math.PI / 4f),
                Config.get().getBoltSpeed(),
                clock.getTime()).orElse(null);
        assertNotNull(bolt);


        when(clock.getTime()).thenReturn(1_000L);

        bolts.fixedUpdate(clock.getTime());

        assertEquals(1f - 0.0070710676f, bolt.getPositionVector().getX());
        assertEquals(0.0070710676f, bolt.getPositionVector().getY());
    }

    @Test
    void testMoveLeft() {
        AtomicReference<DomainEvent> eventReference = new AtomicReference<>(null);
        GameEvents.getDomainEvents().setConsumer(eventReference::set);

        Config.get().setBoltMaxDuration(1_500);
        EntityId one = EntityId.newId();
        final Bolt bolt = bolts.fired(players,
                EntityId.newId(),
                one,
                1f,
                0f,
                (float) Math.PI,
                Config.get().getBoltSpeed(),
                clock.getTime()).orElse(null);
        assertNotNull(bolt);


        when(clock.getTime()).thenReturn(1_000L);

        bolts.fixedUpdate(clock.getTime());

        assertEquals(0.99f, bolt.getPositionVector().getX());
        assertEquals(0f, bolt.getPositionVector().getY());
    }

    @Test
    void testMoveLefUp() {
        AtomicReference<DomainEvent> eventReference = new AtomicReference<>(null);
        GameEvents.getDomainEvents().setConsumer(eventReference::set);

        Config.get().setBoltMaxDuration(1_500);
        EntityId one = EntityId.newId();
        final Bolt bolt = bolts.fired(players,
                EntityId.newId(),
                one,
                1f,
                1f,
                (float) (5 * Math.PI / 4f),
                Config.get().getBoltSpeed(),
                clock.getTime()).orElse(null);
        assertNotNull(bolt);


        when(clock.getTime()).thenReturn(1_000L);

        bolts.fixedUpdate(clock.getTime());

        assertEquals(1f - 0.0070710676f, bolt.getPositionVector().getX());
        assertEquals(1f - 0.0070710676f, bolt.getPositionVector().getY());
    }

    @Test
    void testMoveUp() {
        AtomicReference<DomainEvent> eventReference = new AtomicReference<>(null);
        GameEvents.getDomainEvents().setConsumer(eventReference::set);

        Config.get().setBoltMaxDuration(1_500);
        EntityId one = EntityId.newId();
        final Bolt bolt = bolts.fired(players,
                EntityId.newId(),
                one,
                1f,
                1f,
                (float) (3 * Math.PI / 2f),
                Config.get().getBoltSpeed(),
                clock.getTime()).orElse(null);
        assertNotNull(bolt);

        when(clock.getTime()).thenReturn(1_000L);

        bolts.fixedUpdate(clock.getTime());

        assertEquals(1f, bolt.getPositionVector().getX());
        assertEquals(0.99f, bolt.getPositionVector().getY());
    }

    @Test
    void testCollisionHit() {
        Config.get().setBoltMaxDuration(1_500);

        Player player = mock(Player.class);

        List<Player> playersList = Collections.singletonList(player);
        doAnswer(c -> playersList.iterator()).when(this.players).iterator();
        doAnswer(c -> playersList.spliterator()).when(this.players).spliterator();
        doAnswer(c -> playersList.stream()).when(this.players).stream();
        doAnswer(c -> {
            Consumer<Player> consumer = c.getArgumentAt(0, Consumer.class);
            this.players.stream().forEach(consumer);
            return null;
        }).when(this.players).forEach(any(Consumer.class));

        EntityId one = EntityId.newId();
        final Bolt bolt = bolts.fired(players,
                EntityId.newId(),
                one,
                0.5f,
                0.5f,
                0f,
                Config.get().getBoltSpeed(),
                clock.getTime()).orElse(null);
        assertNotNull(bolt);
        EntityId boltId = bolt.getId();

        doReturn(1_000L).when(clock).getTime();

        doReturn(true)
                .when(player)
                .collision(bolt.getPositionVector(), Config.get().getBoltCollisionRadius());


        List<DomainEvent> domainEvents = new ArrayList<>();
        GameEvents.getDomainEvents().setConsumer(domainEvents::add);

        bolts.fixedUpdate(clock.getTime());

        List<Event> events = domainEvents
                .stream()
                .map(DomainEvent::getEvent)
                .collect(Collectors.toList());


        BoltExhaustedEventDto boltExhaustedEventDto = events
                .stream()
                .filter(Event::hasBoltExhausted)
                .map(Event::getBoltExhausted)
                .findFirst().orElse(null);

        assertNotNull(boltExhaustedEventDto);
        assertEquals(boltId.getGuid().getGuid(), boltExhaustedEventDto.getBoltId().getGuid());

    }

    @Test
    void testCollisionMiss() {
        Config.get().setBoltMaxDuration(1_500);

        Player player = mock(Player.class);

        List<Player> playersList = Collections.singletonList(player);
        doAnswer(c -> playersList.iterator()).when(this.players).iterator();
        doAnswer(c -> playersList.spliterator()).when(this.players).spliterator();
        doAnswer(c -> playersList.stream()).when(this.players).stream();

        EntityId one = EntityId.newId();
        final Bolt bolt = bolts.fired(players,
                EntityId.newId(),
                one,
                0.5f,
                0.5f,
                0f,
                Config.get().getBoltSpeed(),
                clock.getTime()).orElse(null);
        assertNotNull(bolt);


        doReturn(1_000L).when(clock).getTime();
        doReturn(false)
                .when(player)
                .collision(bolt.getPositionVector(), Config.get().getBoltCollisionRadius());

        List<DomainEvent> events = new ArrayList<>();
        GameEvents.getDomainEvents().setConsumer(events::add);

        bolts.fixedUpdate(clock.getTime());


        assertEquals(0, events
                .stream()
                .map(DomainEvent::getEvent)
                .filter(Event::hasBoltExhausted)
                .count());

    }

}