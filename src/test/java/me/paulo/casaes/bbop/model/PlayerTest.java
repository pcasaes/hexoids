package me.paulo.casaes.bbop.model;

import me.paulo.casaes.bbop.dto.BoltFiredEventDto;
import me.paulo.casaes.bbop.dto.BoltMovedEventDto;
import me.paulo.casaes.bbop.dto.CommandType;
import me.paulo.casaes.bbop.dto.Dto;
import me.paulo.casaes.bbop.dto.EventDto;
import me.paulo.casaes.bbop.dto.EventType;
import me.paulo.casaes.bbop.dto.PlayerDto;
import me.paulo.casaes.bbop.dto.PlayerJoinedEventDto;
import me.paulo.casaes.bbop.dto.PlayerLeftEventDto;
import me.paulo.casaes.bbop.dto.PlayerMovedEventDto;
import me.paulo.casaes.bbop.dto.PlayersListCommandDto;
import mockit.MockUp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerTest {

    @Mock
    private Clock clock;

    @Mock
    private ScoreBoard scoreBoard;


    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        GameEvents.getClientEvents().setConsumer(null);
        GameEvents.getDomainEvents().setConsumer(null);

        new MockUp<Clock.Factory>() {
            @mockit.Mock
            public Clock get() {
                return clock;
            }
        };

        new MockUp<ScoreBoard.Factory>() {
            @mockit.Mock
            public ScoreBoard get() {
                return scoreBoard;
            }
        };

        when(clock.getTime()).thenReturn(0L);

        Config.get().setEnv(Config.Environment.DEV.name());
        Config.get().setPlayerMaxMove(1f);
        Config.get().setMinMove(0.000000001f);
        Config.get().setPlayerMaxAngleDivisor(0.5f);

        Players.get().reset();
    }

    @Test
    void testCreate() {
        UUID one = UUID.randomUUID();
        UUID two = UUID.randomUUID();
        Player player = Players.get().createOrGet(one);

        assertTrue(Players.get().stream()
                .anyMatch(p -> p == player));

        assertTrue(player.is(one));
        assertFalse(player.is(two));

        assertSame(player, Players.get().get(one).orElse(null));
        assertEquals(player, Players.get().get(one.toString()).orElse(null));

        assertNotSame(player, Players.get().get(two).orElse(null));
        assertNotEquals(player, Players.get().get(two.toString()).orElse(null));

    }

    @Test
    void testJoin() {
        AtomicReference<DomainEvent> eventReference = new AtomicReference<>(null);
        GameEvents.getDomainEvents().setConsumer(eventReference::set);

        UUID one = UUID.randomUUID();
        Players.get().createOrGet(one).join();
        DomainEvent domainEventDto = eventReference.get();
        assertEquals(Topics.JoinGameTopic.name(), domainEventDto.getTopic());
        assertEquals(one.toString(), domainEventDto.getKey());

        PlayerJoinedEventDto event = (PlayerJoinedEventDto) domainEventDto.getEvent();

        assertNotNull(event);
        assertEquals(EventType.PLAYER_JOINED, event.getEvent());
        assertEquals(one.toString(), event.getPlayerId());
        assertEquals(0f, event.getX());
        assertEquals(0f, event.getY());
        assertEquals(0f, event.getY());
    }

    @Test
    void testJoined() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        UUID one = UUID.randomUUID();

        Players.get().joined(
                PlayerJoinedEventDto.of(one.toString(), 5, 0f, 1f, 2f)
        );
        PlayerJoinedEventDto event = (PlayerJoinedEventDto) eventReference.get();

        assertNotNull(event);
        assertEquals(EventType.PLAYER_JOINED, event.getEvent());
        assertEquals(one.toString(), event.getPlayerId());
        assertEquals(0f, event.getX());
        assertEquals(1f, event.getY());
        assertEquals(2f, event.getAngle());
        assertEquals(5, event.getShip());
    }

    @Test
    void testLeave() {
        List<DomainEvent> domainEvents = new ArrayList<>();
        GameEvents.getDomainEvents().setConsumer(domainEvents::add);

        UUID one = UUID.randomUUID();
        Player player = Players.get().createOrGet(one);
        player.leave();

        assertEquals(2, domainEvents.size());

        assertEquals(Topics.JoinGameTopic.name(), domainEvents.get(0).getTopic());
        assertEquals(one.toString(), domainEvents.get(0).getKey());
        assertNull(domainEvents.get(0).getEvent());

        assertEquals(Topics.PlayerActionTopic.name(), domainEvents.get(1).getTopic());
        assertEquals(one.toString(), domainEvents.get(1).getKey());
        assertNull(domainEvents.get(1).getEvent());


    }

    @Test
    void testLeft() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        UUID one = UUID.randomUUID();
        Player player = Players.get().createOrGet(one);
        Players.get().left(one);
        PlayerLeftEventDto event = (PlayerLeftEventDto) eventReference.get();

        assertNotNull(event);
        assertEquals(EventType.PLAYER_LEFT, event.getEvent());
        assertEquals(one.toString(), event.getPlayerId());

        assertFalse(Players.get().stream()
                .anyMatch(p -> p == player));

    }

    @Test
    void testRequestListOfPlayers() {
        UUID one = UUID.randomUUID();
        UUID two = UUID.randomUUID();
        Players.get().createOrGet(one).join();
        Players.get().createOrGet(two).join();

        PlayersListCommandDto command = Players.get().requestListOfPlayers();

        assertNotNull(command);
        assertEquals(CommandType.LIST_PLAYERS, command.getCommand());
        assertEquals(2, command.getPlayers().size());

        Set<UUID> playerIds = command.getPlayers()
                .stream()
                .map(PlayerDto::getPlayerId)
                .map(UUID::fromString)
                .collect(Collectors.toCollection(HashSet::new));

        assertTrue(playerIds.contains(one));
        assertTrue(playerIds.contains(two));
    }

    @Test
    void testMove() {
        AtomicReference<DomainEvent> eventReference = new AtomicReference<>(null);
        GameEvents.getDomainEvents().setConsumer(eventReference::set);

        UUID one = UUID.randomUUID();
        Player player = Players.get().createOrGet(one);
        player.join();
        player.move(0.1f, 0.2f, (float) Math.PI, (float) Math.PI);

        DomainEvent domainEvent = eventReference.get();
        assertNotNull(domainEvent);
        assertEquals(Topics.PlayerActionTopic.name(), domainEvent.getTopic());
        PlayerMovedEventDto event = (PlayerMovedEventDto) domainEvent.getEvent();

        assertNotNull(event);
        assertEquals(0.1f, event.getX());
        assertEquals(0.2f, event.getY());
        assertEquals((float) Math.PI, event.getAngle());

    }

    @Test
    void testBoundedMove() {
        AtomicReference<DomainEvent> eventReference = new AtomicReference<>(null);
        GameEvents.getDomainEvents().setConsumer(eventReference::set);

        UUID one = UUID.randomUUID();
        Player player = Players.get().createOrGet(one);
        player.join();
        player.move(-1f, 2f, null, null);

        DomainEvent domainEvent = eventReference.get();
        assertNotNull(domainEvent);
        assertEquals(Topics.PlayerActionTopic.name(), domainEvent.getTopic());
        PlayerMovedEventDto event = (PlayerMovedEventDto) domainEvent.getEvent();

        assertNotNull(event);
        assertEquals(0f, event.getX());
        assertEquals(1f, event.getY());
        assertEquals(0f, event.getAngle());

    }

    @Test
    void testMoved() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        UUID one = UUID.randomUUID();
        Player player = Players.get().createOrGet(one);
        player.join();
        player.moved(PlayerMovedEventDto.of(one.toString(), 0f, 1f, 3f, 3f, 4f, 1000L));

        PlayerMovedEventDto event = (PlayerMovedEventDto) eventReference.get();

        assertNotNull(event);
        assertEquals(0f, event.getX());
        assertEquals(1f, event.getY());
        assertEquals(3f, event.getAngle());
        assertEquals(4f, event.getCurrentSpeed());

    }

    @Test
    void testOnlyAngleMove() {
        AtomicReference<DomainEvent> eventReference = new AtomicReference<>(null);
        GameEvents.getDomainEvents().setConsumer(eventReference::set);

        UUID one = UUID.randomUUID();
        Player player = Players.get().createOrGet(one);
        player.join();
        player.move(0f, 0f, (float) Math.PI, (float) Math.PI);

        DomainEvent domainEvent = eventReference.get();
        assertNotNull(domainEvent);
        assertEquals(Topics.PlayerActionTopic.name(), domainEvent.getTopic());
        PlayerMovedEventDto event = (PlayerMovedEventDto) domainEvent.getEvent();

        assertNotNull(event);
        assertEquals(0f, event.getX());
        assertEquals(0f, event.getY());
        assertEquals((float) Math.PI, event.getAngle());

    }

    @Test
    void testNoMove() {
        UUID one = UUID.randomUUID();
        Player player = Players.get().createOrGet(one);
        player.join();

        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        player.move(0f, 0f, null, null);

        assertNull(eventReference.get());
    }

    @Test
    void testMaxFire() {
        Config.get().setMaxBolts(2);

        UUID one = UUID.randomUUID();
        Player player = Players.get().createOrGet(one);
        player.join();

        player.fired(BoltFiredEventDto.of(UUID.randomUUID().toString(), one.toString(), 0, 0, 0f, 0, Clock.Factory.get().getTime()));
        player.fired(BoltFiredEventDto.of(UUID.randomUUID().toString(), one.toString(), 0, 0, 0f, 0, Clock.Factory.get().getTime()));
        player.fired(BoltFiredEventDto.of(UUID.randomUUID().toString(), one.toString(), 0, 0, 0f, 0, Clock.Factory.get().getTime()));


        assertEquals(2, player.getActiveBoltCount());
    }

    @Test
    void testBoltExhaustion() {
        Config.get().setMaxBolts(2);

        UUID one = UUID.randomUUID();
        Player player = Players.get().createOrGet(one);
        player.join();

        player.fired(BoltFiredEventDto.of(UUID.randomUUID().toString(), one.toString(), 0, 0, 0f, 0, Clock.Factory.get().getTime()));

        assertEquals(1, player.getActiveBoltCount());

        player.boltExhausted();

        assertEquals(0, player.getActiveBoltCount());
    }

    @Test
    void testFireDirection() {
        Config.get().setMaxBolts(2);

        UUID one = UUID.randomUUID();
        Player player = Players.get().createOrGet(one);
        player.join();
        player.move(0.5f, 0.5f, (float) Math.PI, (float) Math.PI);

        AtomicReference<DomainEvent> eventReference = new AtomicReference<>(null);
        GameEvents.getDomainEvents().setConsumer(eventReference::set);

        player.fire();

        DomainEvent event = eventReference.get();
        assertNotNull(event);
        assertEquals(EventType.BOLT_FIRED, event.getEvent().getEvent());

        player.fired((BoltFiredEventDto) event.getEvent());

        assertEquals(1, Bolts.get()
                .stream()
                .filter(b -> b.isOwnedBy(one))
                .map(Bolt::generateMovedEvent)
                .map(DomainEvent::getEvent)
                .map(b -> (BoltMovedEventDto) b)
                .filter(b -> b.getAngle() == (float) Math.PI)
                .count());

    }

    @Test
    void testCollisionHitBullsEye() {
        UUID one = UUID.randomUUID();
        Player player = Players.get().createOrGet(one);
        player.join();
        player.move(0.5f, 0.5f, null, null);

        assertTrue(player.collision(0.45f, 0.45f, 0.55f, 0.55f, 0.05f));
    }

    @Test
    void testCollisionHitWithinRadius() {
        UUID one = UUID.randomUUID();
        Player player = Players.get().createOrGet(one);
        player.join();
        player.move(0.54f, 0.54f, null, null);

        assertTrue(player.collision(0.45f, 0.45f, 0.55f, 0.55f, 0.05f));
    }

    @Test
    void testCollisionNoHitInsideSquare() {
        UUID one = UUID.randomUUID();
        Player player = Players.get().createOrGet(one);
        player.join();
        player.move(0.4f, 0.6f, null, null);

        assertFalse(player.collision(0.45f, 0.45f, 0.55f, 0.55f, 0.05f));
    }

    @Test
    void testCollisionNoHitOutsideSquareX() {
        UUID one = UUID.randomUUID();
        Player player = Players.get().createOrGet(one);
        player.join();
        player.move(0.2f, 0.2f, null, null);

        assertFalse(player.collision(0.45f, 0.45f, 0.55f, 0.55f, 0.05f));
    }

    @Test
    void testCollisionNoHitOutsideSquareY() {
        UUID one = UUID.randomUUID();
        Player player = Players.get().createOrGet(one);
        player.join();
        player.move(0.5f, 0.2f, null, null);

        assertFalse(player.collision(0.45f, 0.45f, 0.55f, 0.55f, 0.05f));
    }

    @Test
    void testDestroyedBy() {
        UUID one = UUID.randomUUID();
        UUID two = UUID.randomUUID();
        Player player1 = Players.get().createOrGet(one);
        player1.join();
        Player player2 = Players.get().createOrGet(two);
        player2.join();

        player1.move(0.5f, 0.5f, 1f, 1f);

        List<DomainEvent> domainEvents = new ArrayList<>();
        GameEvents.getDomainEvents().setConsumer(domainEvents::add);

        player1.destroyedBy(two);

        List<EventDto> events = domainEvents
                .stream()
                .map(DomainEvent::getEvent)
                .collect(Collectors.toList());


        assertEquals(2, events.size());
        assertEquals(EventType.PLAYER_DESTROYED, events.get(0).getEvent());

        assertEquals(EventType.PLAYER_MOVED, events.get(1).getEvent());

        PlayerMovedEventDto playerMovedEventDto = (PlayerMovedEventDto) events.get(1);
        assertEquals(0f, playerMovedEventDto.getX());
        assertEquals(0f, playerMovedEventDto.getY());
        assertEquals(0f, playerMovedEventDto.getAngle());

        verify(scoreBoard, times(1)).updateScore(two, 1);

    }
}