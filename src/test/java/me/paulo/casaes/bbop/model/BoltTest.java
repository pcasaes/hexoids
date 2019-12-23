package me.paulo.casaes.bbop.model;

import me.paulo.casaes.bbop.dto.BoltExhaustedEventDto;
import me.paulo.casaes.bbop.dto.BoltMovedEventDto;
import me.paulo.casaes.bbop.dto.Dto;
import me.paulo.casaes.bbop.dto.EventDto;
import me.paulo.casaes.bbop.dto.EventType;
import mockit.MockUp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BoltTest {

    @Mock
    private Clock clock;

    @Mock
    private Players players;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        new MockUp<Clock.Factory>() {
            @mockit.Mock
            public Clock get() {
                return clock;
            }
        };

        new MockUp<Players>() {
            @mockit.Mock
            public Players get() {
                return players;
            }
        };

        GameEvents.getClientEvents().setConsumer(null);
        GameEvents.getDomainEvents().setConsumer(null);

        Bolt.reset();

        when(clock.getTime()).thenReturn(0L);

        Config.get().setEnv(Config.Environment.DEV.name());
        Config.get().setBoltMaxDuration(10_000L);
        Config.get().setBoltSpeed(0.01f);
        Config.get().setBoltCollisionRadius(0.001f);
        Config.get().setMinMove(0.000000001f);

        doReturn(Collections.emptyList()).when(players).iterable();
    }

    @Test
    void testFireMoveAndExhaust() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        Config.get().setBoltMaxDuration(1_500L);
        final Bolt bolt = Bolt.fire("1", 0f, 0f, 0f, 0f);

        assertTrue(bolt.isOwnedBy("1"));

        assertEquals(1, StreamSupport
                .stream(Bolt.iterable().spliterator(), false)
                .count());

        assertTrue(StreamSupport
                .stream(Bolt.iterable().spliterator(), false)
                .anyMatch(b -> bolt == b));

        assertFalse(bolt.isExhausted());
        assertTrue(bolt.isActive());

        when(clock.getTime()).thenReturn(1_000L);

        Bolt.fixedUpdate(clock.getTime());
        BoltMovedEventDto movedEvent = (BoltMovedEventDto) eventReference.get();

        assertNotNull(movedEvent);
        assertTrue(bolt.is(UUID.fromString(movedEvent.getBoltId())));

        assertFalse(bolt.isExhausted());
        assertTrue(bolt.isActive());

        when(clock.getTime()).thenReturn(2_000L);

        Bolt.fixedUpdate(clock.getTime());
        BoltExhaustedEventDto exhaustedEvent = (BoltExhaustedEventDto) eventReference.get();

        assertNotNull(exhaustedEvent);
        assertTrue(bolt.is(UUID.fromString(exhaustedEvent.getBoltId())));

        assertTrue(bolt.isExhausted());
        assertFalse(bolt.isActive());

        assertEquals(0, StreamSupport
                .stream(Bolt.iterable().spliterator(), false)
                .count());
    }

    @Test
    void testMoveRight() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        Config.get().setBoltMaxDuration(1_500L);
        final Bolt bolt = Bolt.fire("1", 0f, 0f, 0f, 0f);


        when(clock.getTime()).thenReturn(1_000L);

        Bolt.fixedUpdate(clock.getTime());
        BoltMovedEventDto movedEvent = (BoltMovedEventDto) eventReference.get();

        assertNotNull(movedEvent);
        assertTrue(bolt.is(UUID.fromString(movedEvent.getBoltId())));
        assertEquals("1", movedEvent.getOwnerPlayerId());
        assertEquals(0.01f, movedEvent.getX());
        assertEquals(0f, movedEvent.getY());

    }

    @Test
    void testMoveRightDown() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        Config.get().setBoltMaxDuration(1_500L);
        final Bolt bolt = Bolt.fire("1", 0f, 0f, (float) Math.PI / 4f, 0f);


        when(clock.getTime()).thenReturn(1_000L);

        Bolt.fixedUpdate(clock.getTime());
        BoltMovedEventDto movedEvent = (BoltMovedEventDto) eventReference.get();

        assertNotNull(movedEvent);
        assertTrue(bolt.is(UUID.fromString(movedEvent.getBoltId())));
        assertEquals("1", movedEvent.getOwnerPlayerId());
        assertEquals(0.0070710676f, movedEvent.getX());
        assertEquals(0.0070710676f, movedEvent.getY());
    }

    @Test
    void testMoveDown() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        Config.get().setBoltMaxDuration(1_500L);
        final Bolt bolt = Bolt.fire("1", 0f, 0f, (float) Math.PI / 2f, 0f);


        when(clock.getTime()).thenReturn(1_000L);

        Bolt.fixedUpdate(clock.getTime());
        BoltMovedEventDto movedEvent = (BoltMovedEventDto) eventReference.get();

        assertNotNull(movedEvent);
        assertTrue(bolt.is(UUID.fromString(movedEvent.getBoltId())));
        assertEquals("1", movedEvent.getOwnerPlayerId());
        assertEquals(0f, movedEvent.getX());
        assertEquals(0.01f, movedEvent.getY());
    }

    @Test
    void testMoveLeftDown() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        Config.get().setBoltMaxDuration(1_500L);
        final Bolt bolt = Bolt.fire("1", 1f, 0f, (float) (3 * Math.PI / 4f), 0f);


        when(clock.getTime()).thenReturn(1_000L);

        Bolt.fixedUpdate(clock.getTime());
        BoltMovedEventDto movedEvent = (BoltMovedEventDto) eventReference.get();

        assertNotNull(movedEvent);
        assertTrue(bolt.is(UUID.fromString(movedEvent.getBoltId())));
        assertEquals("1", movedEvent.getOwnerPlayerId());
        assertEquals(1f - 0.0070710676f, movedEvent.getX());
        assertEquals(0.0070710676f, movedEvent.getY());
    }

    @Test
    void testMoveLeft() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        Config.get().setBoltMaxDuration(1_500L);
        final Bolt bolt = Bolt.fire("1", 1f, 0f, (float) Math.PI, 0f);


        when(clock.getTime()).thenReturn(1_000L);

        Bolt.fixedUpdate(clock.getTime());
        BoltMovedEventDto movedEvent = (BoltMovedEventDto) eventReference.get();

        assertNotNull(movedEvent);
        assertTrue(bolt.is(UUID.fromString(movedEvent.getBoltId())));
        assertEquals("1", movedEvent.getOwnerPlayerId());
        assertEquals(0.99f, movedEvent.getX());
        assertEquals(0f, movedEvent.getY());
    }

    @Test
    void testMoveLefUp() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        Config.get().setBoltMaxDuration(1_500L);
        final Bolt bolt = Bolt.fire("1", 1f, 1f, (float) (5 * Math.PI / 4), 0f);


        when(clock.getTime()).thenReturn(1_000L);

        Bolt.fixedUpdate(clock.getTime());
        BoltMovedEventDto movedEvent = (BoltMovedEventDto) eventReference.get();

        assertNotNull(movedEvent);
        assertTrue(bolt.is(UUID.fromString(movedEvent.getBoltId())));
        assertEquals("1", movedEvent.getOwnerPlayerId());
        assertEquals(1f - 0.0070710676f, movedEvent.getX());
        assertEquals(1f - 0.0070710676f, movedEvent.getY());
    }

    @Test
    void testMoveUp() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        Config.get().setBoltMaxDuration(1_500L);
        final Bolt bolt = Bolt.fire("1", 1f, 1f, (float) (3 * Math.PI / 2), 0f);


        when(clock.getTime()).thenReturn(1_000L);

        Bolt.fixedUpdate(clock.getTime());
        BoltMovedEventDto movedEvent = (BoltMovedEventDto) eventReference.get();

        assertNotNull(movedEvent);
        assertTrue(bolt.is(UUID.fromString(movedEvent.getBoltId())));
        assertEquals("1", movedEvent.getOwnerPlayerId());
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
        doReturn(playersList).when(this.players).iterable();

        final Bolt bolt = Bolt.fire("1", 0.5f, 0.5f, 0f, 0f);


        doReturn(1_000L).when(clock).getTime();

        List<Dto> dtos = new ArrayList<>();
        GameEvents.getClientEvents().setConsumer(dtos::add);

        Bolt.fixedUpdate(clock.getTime());

        List<EventDto> events = dtos
                .stream()
                .map(ev -> (EventDto) ev)
                .collect(Collectors.toList());

        BoltMovedEventDto boltMovedEventDto = (BoltMovedEventDto) events
                .stream()
                .filter(ev -> ev.isEvent(EventType.BOLT_MOVED))
                .findFirst().orElse(null);

        assertNotNull(boltMovedEventDto);
        assertEquals("1", boltMovedEventDto.getOwnerPlayerId());

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
        doReturn(playersList).when(this.players).iterable();

        final Bolt bolt = Bolt.fire("1", 0.5f, 0.5f, 0f, 0f);


        doReturn(1_000L).when(clock).getTime();

        List<Dto> events = new ArrayList<>();
        GameEvents.getClientEvents().setConsumer(events::add);

        Bolt.fixedUpdate(clock.getTime());

        BoltMovedEventDto boltMovedEventDto = (BoltMovedEventDto) events
                .stream()
                .map(ev -> (EventDto) ev)
                .filter(ev -> ev.isEvent(EventType.BOLT_MOVED))
                .findFirst().orElse(null);

        assertNotNull(boltMovedEventDto);
        assertEquals("1", boltMovedEventDto.getOwnerPlayerId());

    }

}