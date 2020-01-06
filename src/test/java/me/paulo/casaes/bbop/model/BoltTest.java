package me.paulo.casaes.bbop.model;

import me.paulo.casaes.bbop.dto.BoltExhaustedEventDto;
import me.paulo.casaes.bbop.dto.BoltMovedEventDto;
import me.paulo.casaes.bbop.dto.EventDto;
import me.paulo.casaes.bbop.dto.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
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

        Config.get().setEnv(Config.Environment.DEV.name());
        Config.get().setBoltMaxDuration(10_000L);
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

        UUID one = UUID.randomUUID();
        Config.get().setBoltMaxDuration(1_500L);
        final Bolt bolt = bolts.fired(players, UUID.randomUUID(), one, 0f, 0f, 0f, clock.getTime()).orElse(null);
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
        assertEquals(1, events.size());
        DomainEvent domainEvent = events.get(0);
        assertNotNull(domainEvent);
        events.clear();

        BoltMovedEventDto movedEvent = (BoltMovedEventDto) domainEvent.getEvent();

        assertNotNull(movedEvent);
        assertTrue(bolt.is(UUID.fromString(movedEvent.getBoltId())));

        assertFalse(bolt.isExhausted());
        assertTrue(bolt.isActive());

        when(clock.getTime()).thenReturn(2_000L);

        bolts.fixedUpdate(clock.getTime());
        assertEquals(1, events.size());
        domainEvent = events.get(0);
        assertNotNull(domainEvent);

        BoltExhaustedEventDto exhaustedEvent = (BoltExhaustedEventDto) domainEvent.getEvent();

        assertNotNull(exhaustedEvent);
        assertTrue(bolt.is(UUID.fromString(exhaustedEvent.getBoltId())));

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

        Config.get().setBoltMaxDuration(1_500L);
        UUID one = UUID.randomUUID();
        final Bolt bolt = bolts.fired(players, UUID.randomUUID(), one, 0f, 0f, 0f, clock.getTime()).orElse(null);
        assertNotNull(bolt);


        when(clock.getTime()).thenReturn(1_000L);

        bolts.fixedUpdate(clock.getTime());
        DomainEvent domainEvent = eventReference.get();
        assertNotNull(domainEvent);
        BoltMovedEventDto movedEvent = (BoltMovedEventDto) domainEvent.getEvent();

        assertNotNull(movedEvent);
        assertTrue(bolt.is(UUID.fromString(movedEvent.getBoltId())));
        assertEquals(one.toString(), movedEvent.getOwnerPlayerId());
        assertEquals(0.01f, movedEvent.getX());
        assertEquals(0f, movedEvent.getY());

    }

    @Test
    void testMoveRightDown() {
        AtomicReference<DomainEvent> eventReference = new AtomicReference<>(null);
        GameEvents.getDomainEvents().setConsumer(eventReference::set);

        Config.get().setBoltMaxDuration(1_500L);
        UUID one = UUID.randomUUID();
        final Bolt bolt = bolts.fired(players, UUID.randomUUID(), one, 0f, 0f, (float) Math.PI / 4f, clock.getTime()).orElse(null);
        assertNotNull(bolt);


        when(clock.getTime()).thenReturn(1_000L);

        bolts.fixedUpdate(clock.getTime());
        DomainEvent domainEvent = eventReference.get();
        assertNotNull(domainEvent);
        BoltMovedEventDto movedEvent = (BoltMovedEventDto) domainEvent.getEvent();

        assertNotNull(movedEvent);
        assertTrue(bolt.is(UUID.fromString(movedEvent.getBoltId())));
        assertEquals(one.toString(), movedEvent.getOwnerPlayerId());
        assertEquals(0.0070710676f, movedEvent.getX());
        assertEquals(0.0070710676f, movedEvent.getY());
    }

    @Test
    void testMoveDown() {
        AtomicReference<DomainEvent> eventReference = new AtomicReference<>(null);
        GameEvents.getDomainEvents().setConsumer(eventReference::set);

        Config.get().setBoltMaxDuration(1_500L);
        UUID one = UUID.randomUUID();
        final Bolt bolt = bolts.fired(players, UUID.randomUUID(), one, 0f, 0f, (float) Math.PI / 2f, clock.getTime()).orElse(null);
        assertNotNull(bolt);


        when(clock.getTime()).thenReturn(1_000L);

        bolts.fixedUpdate(clock.getTime());
        DomainEvent domainEvent = eventReference.get();
        assertNotNull(domainEvent);
        BoltMovedEventDto movedEvent = (BoltMovedEventDto) domainEvent.getEvent();

        assertNotNull(movedEvent);
        assertTrue(bolt.is(UUID.fromString(movedEvent.getBoltId())));
        assertEquals(one.toString(), movedEvent.getOwnerPlayerId());
        assertEquals(0f, movedEvent.getX());
        assertEquals(0.01f, movedEvent.getY());
    }

    @Test
    void testMoveLeftDown() {
        AtomicReference<DomainEvent> eventReference = new AtomicReference<>(null);
        GameEvents.getDomainEvents().setConsumer(eventReference::set);

        Config.get().setBoltMaxDuration(1_500L);
        UUID one = UUID.randomUUID();
        final Bolt bolt = bolts.fired(players, UUID.randomUUID(), one, 1f, 0f, (float) (3 * Math.PI / 4f), clock.getTime()).orElse(null);
        assertNotNull(bolt);


        when(clock.getTime()).thenReturn(1_000L);

        bolts.fixedUpdate(clock.getTime());
        DomainEvent domainEvent = eventReference.get();
        assertNotNull(domainEvent);

        BoltMovedEventDto movedEvent = (BoltMovedEventDto) domainEvent.getEvent();

        assertNotNull(movedEvent);
        assertTrue(bolt.is(UUID.fromString(movedEvent.getBoltId())));
        assertEquals(one.toString(), movedEvent.getOwnerPlayerId());
        assertEquals(1f - 0.0070710676f, movedEvent.getX());
        assertEquals(0.0070710676f, movedEvent.getY());
    }

    @Test
    void testMoveLeft() {
        AtomicReference<DomainEvent> eventReference = new AtomicReference<>(null);
        GameEvents.getDomainEvents().setConsumer(eventReference::set);

        Config.get().setBoltMaxDuration(1_500L);
        UUID one = UUID.randomUUID();
        final Bolt bolt = bolts.fired(players, UUID.randomUUID(), one, 1f, 0f, (float) Math.PI, clock.getTime()).orElse(null);
        assertNotNull(bolt);


        when(clock.getTime()).thenReturn(1_000L);

        bolts.fixedUpdate(clock.getTime());
        DomainEvent domainEvent = eventReference.get();
        assertNotNull(domainEvent);
        BoltMovedEventDto movedEvent = (BoltMovedEventDto) domainEvent.getEvent();

        assertNotNull(movedEvent);
        assertTrue(bolt.is(UUID.fromString(movedEvent.getBoltId())));
        assertEquals(one.toString(), movedEvent.getOwnerPlayerId());
        assertEquals(0.99f, movedEvent.getX());
        assertEquals(0f, movedEvent.getY());
    }

    @Test
    void testMoveLefUp() {
        AtomicReference<DomainEvent> eventReference = new AtomicReference<>(null);
        GameEvents.getDomainEvents().setConsumer(eventReference::set);

        Config.get().setBoltMaxDuration(1_500L);
        UUID one = UUID.randomUUID();
        final Bolt bolt = bolts.fired(players, UUID.randomUUID(), one, 1f, 1f, (float) (5 * Math.PI / 4), clock.getTime()).orElse(null);
        assertNotNull(bolt);


        when(clock.getTime()).thenReturn(1_000L);

        bolts.fixedUpdate(clock.getTime());
        DomainEvent domainEvent = eventReference.get();
        assertNotNull(domainEvent);
        BoltMovedEventDto movedEvent = (BoltMovedEventDto) domainEvent.getEvent();

        assertNotNull(movedEvent);
        assertTrue(bolt.is(UUID.fromString(movedEvent.getBoltId())));
        assertEquals(one.toString(), movedEvent.getOwnerPlayerId());
        assertEquals(1f - 0.0070710676f, movedEvent.getX());
        assertEquals(1f - 0.0070710676f, movedEvent.getY());
    }

    @Test
    void testMoveUp() {
        AtomicReference<DomainEvent> eventReference = new AtomicReference<>(null);
        GameEvents.getDomainEvents().setConsumer(eventReference::set);

        Config.get().setBoltMaxDuration(1_500L);
        UUID one = UUID.randomUUID();
        final Bolt bolt = bolts.fired(players, UUID.randomUUID(), one, 1f, 1f, (float) (3 * Math.PI / 2), clock.getTime()).orElse(null);
        assertNotNull(bolt);

        when(clock.getTime()).thenReturn(1_000L);

        bolts.fixedUpdate(clock.getTime());
        DomainEvent domainEvent = eventReference.get();
        assertNotNull(domainEvent);
        BoltMovedEventDto movedEvent = (BoltMovedEventDto) domainEvent.getEvent();

        assertNotNull(movedEvent);
        assertTrue(bolt.is(UUID.fromString(movedEvent.getBoltId())));
        assertEquals(one.toString(), movedEvent.getOwnerPlayerId());
        assertEquals(1f, movedEvent.getX());
        assertEquals(0.99f, movedEvent.getY());
    }

    @Test
    void testCollisionHit() {
        Config.get().setBoltMaxDuration(1_500L);

        Player player = mock(Player.class);
        doReturn(true)
                .when(player)
                .collision(0.5f, 0.5f, 0.51f, 0.5f, Config.get().getBoltCollisionRadius());

        List<Player> playersList = Collections.singletonList(player);
        doAnswer(c -> playersList.iterator()).when(this.players).iterator();
        doAnswer(c -> playersList.spliterator()).when(this.players).spliterator();
        doAnswer(c -> playersList.stream()).when(this.players).stream();
        doAnswer(c -> {
            Consumer<Player> consumer = c.getArgumentAt(0, Consumer.class);
            this.players.stream().forEach(consumer);
            return null;
        }).when(this.players).forEach(any(Consumer.class));

        UUID one = UUID.randomUUID();
        final Bolt bolt = bolts.fired(players, UUID.randomUUID(), one, 0.5f, 0.5f, 0f, clock.getTime()).orElse(null);
        assertNotNull(bolt);

        doReturn(1_000L).when(clock).getTime();

        List<DomainEvent> domainEvents = new ArrayList<>();
        GameEvents.getDomainEvents().setConsumer(domainEvents::add);

        bolts.fixedUpdate(clock.getTime());

        List<EventDto> events = domainEvents
                .stream()
                .map(DomainEvent::getEvent)
                .collect(Collectors.toList());

        BoltMovedEventDto boltMovedEventDto = (BoltMovedEventDto) events
                .stream()
                .filter(ev -> ev.isEvent(EventType.BOLT_MOVED))
                .findFirst().orElse(null);

        assertNotNull(boltMovedEventDto);
        assertEquals(one.toString(), boltMovedEventDto.getOwnerPlayerId());

        BoltExhaustedEventDto boltExhaustedEventDto = (BoltExhaustedEventDto) events
                .stream()
                .filter(ev -> ev.isEvent(EventType.BOLT_EXHAUSTED))
                .findFirst().orElse(null);

        assertNotNull(boltExhaustedEventDto);
        assertTrue(bolt.is(UUID.fromString(boltExhaustedEventDto.getBoltId())));

    }

    @Test
    void testCollisionMiss() {
        Config.get().setBoltMaxDuration(1_500L);

        Player player = mock(Player.class);
        doReturn(false)
                .when(player)
                .collision(0.5f, 0.5f, 0.51f, 0.5f, Config.get().getBoltCollisionRadius());

        List<Player> playersList = Collections.singletonList(player);
        doAnswer(c -> playersList.iterator()).when(this.players).iterator();
        doAnswer(c -> playersList.spliterator()).when(this.players).spliterator();
        doAnswer(c -> playersList.stream()).when(this.players).stream();

        UUID one = UUID.randomUUID();
        final Bolt bolt = bolts.fired(players, UUID.randomUUID(), one, 0.5f, 0.5f, 0f, clock.getTime()).orElse(null);
        assertNotNull(bolt);


        doReturn(1_000L).when(clock).getTime();

        List<DomainEvent> events = new ArrayList<>();
        GameEvents.getDomainEvents().setConsumer(events::add);

        bolts.fixedUpdate(clock.getTime());


        BoltMovedEventDto boltMovedEventDto = (BoltMovedEventDto) events
                .stream()
                .map(DomainEvent::getEvent)
                .filter(ev -> ev.isEvent(EventType.BOLT_MOVED))
                .findFirst().orElse(null);

        assertNotNull(boltMovedEventDto);
        assertEquals(one.toString(), boltMovedEventDto.getOwnerPlayerId());

    }

}