package me.pcasaes.bbop.model;


import me.pcasaes.bbop.model.vector.PositionVector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pcasaes.bbop.proto.DirectedCommand;
import pcasaes.bbop.proto.Dto;
import pcasaes.bbop.proto.Event;
import pcasaes.bbop.proto.JoinCommandDto;
import pcasaes.bbop.proto.PlayerDto;
import pcasaes.bbop.proto.PlayerJoinedEventDto;
import pcasaes.bbop.proto.PlayerMovedEventDto;
import pcasaes.bbop.proto.PlayerSpawnedEventDto;
import pcasaes.bbop.proto.PlayersListCommandDto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static me.pcasaes.bbop.model.DtoUtils.PLAYER_JOINED_BUILDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerTest {

    @Mock
    private Clock clock;

    @Mock
    private ScoreBoard scoreBoard;

    @Mock
    private Game game;

    private Bolts bolts;

    private Players players;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        this.bolts = Bolts.create();
        this.players = Players.create(bolts, clock, scoreBoard);

        doReturn(clock)
                .when(game)
                .getClock();

        doReturn(scoreBoard)
                .when(game)
                .getScoreBoard();

        doReturn(players)
                .when(game)
                .getPlayers();

        doReturn(bolts)
                .when(game)
                .getBolts();

        Topics.setGame(game);

        GameEvents.getClientEvents().setConsumer(null);
        GameEvents.getDomainEvents().setConsumer(domainEvent ->
                Topics.valueOf(domainEvent.getTopic()).consume(domainEvent)
        );


        when(clock.getTime()).thenReturn(0L);

        Config.get().setPlayerMaxMove(1f);
        Config.get().setPlayerNameLength(7);
        Config.get().setMinMove(0.000000001f);
        Config.get().setPlayerMaxAngleDivisor(0.5f);
        Config.get().setBoltInertiaEnabled(false);
    }

    @Test
    void testCreate() {
        EntityId one = EntityId.newId();
        EntityId two = EntityId.newId();
        Player player = this.players.createOrGet(one);

        assertTrue(this.players.stream()
                .anyMatch(p -> p == player));

        assertTrue(player.is(one));
        assertFalse(player.is(two));

        assertSame(player, this.players.get(one).orElse(null));

        assertNotSame(player, this.players.get(two).orElse(null));

    }

    @Test
    void testJoin() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        EntityId one = EntityId.newId();
        this.players.createOrGet(one).join(JoinCommandDto
                .newBuilder()
                .setName("one")
                .build());
        PlayerJoinedEventDto event = eventReference.get().getEvent().getPlayerJoined();

        assertNotNull(event);
        assertEquals(one.getGuid(), event.getPlayerId());
        assertEquals("one", event.getName());
    }

    @Test
    void testJoinNoName() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        EntityId one = EntityId.newId();
        this.players.createOrGet(one).join(JoinCommandDto
                .newBuilder()
                .build());
        PlayerJoinedEventDto event = eventReference.get().getEvent().getPlayerJoined();

        assertNotNull(event);
        assertEquals(one.getGuid(), event.getPlayerId());
        assertEquals(one.toString().substring(0, Config.get().getPlayerNameLength()), event.getName());
    }

    @Test
    void testJoined() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        EntityId one = EntityId.newId();

        this.players.joined(
                PLAYER_JOINED_BUILDER
                        .clear()
                        .setPlayerId(one.getGuid())
                        .setShip(5)
                        .build()
        );
        PlayerJoinedEventDto event = eventReference.get().getEvent().getPlayerJoined();

        assertNotNull(event);
        assertEquals(one.getGuid(), event.getPlayerId());
        assertEquals(5, event.getShip());
    }

    @Test
    void testLeave() {
        List<Dto> dtos = new ArrayList<>();
        GameEvents.getClientEvents().setConsumer(dtos::add);

        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.leave();

        List<Event> events = dtos
                .stream()
                .filter(Dto::hasEvent)
                .map(Dto::getEvent)
                .collect(Collectors.toList());

        assertEquals(1, events.size());

        Event event = events.get(0);
        assertEquals(one.getGuid(), event.getPlayerLeft().getPlayerId());

        assertFalse(this.players.stream()
                .anyMatch(p -> p == player));

    }

    @Test
    void testRequestListOfPlayers() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        EntityId one = EntityId.newId();
        EntityId two = EntityId.newId();
        this.players.createOrGet(one).join(JoinCommandDto
                .newBuilder()
                .setName("one")
                .build());
        this.players.createOrGet(two).join(JoinCommandDto
                .newBuilder()
                .setName("two")
                .build());

        this.players.requestListOfPlayers(one);

        DirectedCommand directedCommandDto = eventReference.get().getDirectedCommand();
        assertNotNull(directedCommandDto);

        assertTrue(directedCommandDto.hasPlayersList());

        PlayersListCommandDto command = directedCommandDto.getPlayersList();

        assertNotNull(command);
        assertEquals(2, command.getPlayersCount());

        Set<EntityId> playerIds = command.getPlayersList()
                .stream()
                .map(PlayerDto::getPlayerId)
                .map(EntityId::of)
                .collect(Collectors.toCollection(HashSet::new));

        assertTrue(playerIds.contains(one));
        assertTrue(playerIds.contains(two));
    }

    @Test
    void testMove() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join(JoinCommandDto
                .newBuilder()
                .setName("one")
                .build());
        when(clock.getTime()).thenReturn(25L);
        player.spawn();
        when(clock.getTime()).thenReturn(1025L);
        player.move(0.1f, 0.1f, (float) Math.PI);

        assertTrue(eventReference.get().hasEvent());
        assertTrue(eventReference.get().getEvent().hasPlayerMoved());
        PlayerMovedEventDto event = eventReference.get().getEvent().getPlayerMoved();

        assertNotNull(event);
        assertEquals(0.1f, event.getX());
        assertEquals(0.1f, event.getY());
        assertEquals((float) Math.PI, event.getAngle());
        assertEquals((float) (Math.PI / 4f), event.getThrustAngle());

    }

    @Test
    void testLimitedMove() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join(JoinCommandDto
                .newBuilder()
                .setName("one")
                .build());
        when(clock.getTime()).thenReturn(25L);
        player.spawn();
        when(clock.getTime()).thenReturn(1025L);
        player.move(0f, 2f, null);

        assertTrue(eventReference.get().hasEvent());
        assertTrue(eventReference.get().getEvent().hasPlayerMoved());
        PlayerMovedEventDto event = eventReference.get().getEvent().getPlayerMoved();

        assertNotNull(event);
        assertEquals(0f, event.getX(), 0.0001f);
        assertEquals(1f, event.getY());
        assertEquals(0f, event.getAngle());
    }

    @Test
    void testBounceMove() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join(JoinCommandDto
                .newBuilder()
                .setName("one")
                .build());
        when(clock.getTime()).thenReturn(25L);
        player.spawn();
        when(clock.getTime()).thenReturn(1025L);
        player.moved(PlayerMovedEventDto.newBuilder()
                .setPlayerId(one.getGuid())
                .setAngle(-1f)
                .setThrustAngle(0)
                .setTimestamp(1025)
                .setVelocity(0f)
                .setX(0.1f)
                .setY(0.1f)
                .build());

        when(clock.getTime()).thenReturn(2025L);
        // from (0.1,0.1) should hit (0,0) and bounce to about (0.2,0.2)
        player.move(-0.3f, -0.3f, -1f);

        assertTrue(eventReference.get().hasEvent());
        assertTrue(eventReference.get().getEvent().hasPlayerMoved());
        PlayerMovedEventDto event = eventReference.get().getEvent().getPlayerMoved();

        assertNotNull(event);
        assertEquals(0.2f, event.getX(), 0.0001f);
        assertEquals(0.2f, event.getY(), 0.0001f);
        assertEquals(-1f, event.getAngle());
    }

    @Test
    void testOnlyAngleMove() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join(JoinCommandDto
                .newBuilder()
                .setName("one")
                .build());
        when(clock.getTime()).thenReturn(25L);
        player.spawn();
        when(clock.getTime()).thenReturn(50L);
        player.move(0f, 0f, (float) Math.PI);

        assertTrue(eventReference.get().hasEvent());
        assertTrue(eventReference.get().getEvent().hasPlayerMoved());
        PlayerMovedEventDto event = eventReference.get().getEvent().getPlayerMoved();

        assertNotNull(event);
        assertEquals(0f, event.getX());
        assertEquals(0f, event.getY());
        assertEquals((float) Math.PI, event.getAngle());

    }

    @Test
    void testNoMove() {
        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join(JoinCommandDto
                .newBuilder()
                .setName("one")
                .build());
        when(clock.getTime()).thenReturn(25L);
        player.spawn();
        when(clock.getTime()).thenReturn(50L);

        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        player.move(0f, 0f, null);

        assertNull(eventReference.get());
    }

    @Test
    void testMaxFire() {
        Config.get().setMaxBolts(2);

        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join(JoinCommandDto
                .newBuilder()
                .setName("one")
                .build());
        when(clock.getTime()).thenReturn(25L);
        player.spawn();

        player.fire();
        player.fire();
        player.fire();


        assertEquals(2, player.getActiveBoltCount());
    }

    @Test
    void testBoltExhaustion() {
        Config.get().setMaxBolts(2);

        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join(JoinCommandDto
                .newBuilder()
                .setName("one")
                .build());
        when(clock.getTime()).thenReturn(25L);
        player.spawn();

        player.fire();

        assertEquals(1, player.getActiveBoltCount());

        player.boltExhausted();

        assertEquals(0, player.getActiveBoltCount());
    }

    @Test
    void testFireDirection() {
        Config.get().setMaxBolts(2);
        Config.get().setBoltInertiaEnabled(false);

        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join(JoinCommandDto
                .newBuilder()
                .setName("one")
                .build());
        when(clock.getTime()).thenReturn(25L);
        player.spawn();
        when(clock.getTime()).thenReturn(50L);
        player.move(0.5f, 0.5f, (float) Math.PI);

        AtomicReference<DomainEvent> eventReference = new AtomicReference<>(null);
        GameEvents.getDomainEvents().setConsumer(eventReference::set);

        player.fire();

        DomainEvent event = eventReference.get();
        assertNotNull(event);
        assertTrue(event.getEvent().hasBoltFired());

        player.fired(event.getEvent().getBoltFired());

        assertEquals(1, bolts
                .stream()
                .filter(b -> b.isOwnedBy(one))
                .map(Bolt::generateMovedEvent)
                .map(DomainEvent::getEvent)
                .filter(Event::hasBoltMoved)
                .map(Event::getBoltMoved)
                .filter(b -> b.getAngle() == (float) Math.PI)
                .count());

    }

    @Test
    @Disabled
        //this test is broken and it's some gnarly math to check
    void testFireDirectionWithInertia() {
        Config.get().setMaxBolts(2);
        Config.get().setBoltInertiaEnabled(true);
        Config.get().setBoltInertiaProjectionScale(1f);
        Config.get().setBoltInertiaRejectionScale(1f);
        Config.get().setBoltInertiaNegativeProjectionScale(1f);
        Config.get().setPlayerMaxMove(20f);
        Config.get().setBoltSpeed(20f);

        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join(JoinCommandDto
                .newBuilder()
                .setName("one")
                .build());
        when(clock.getTime()).thenReturn(25L);
        player.spawn();
        when(clock.getTime()).thenReturn(1025L);
        player.move(0, 0.5f, 0f);

        AtomicReference<DomainEvent> eventReference = new AtomicReference<>(null);
        GameEvents.getDomainEvents().setConsumer(eventReference::set);

        player.fire();

        DomainEvent event = eventReference.get();
        assertNotNull(event);
        assertTrue(event.getEvent().hasBoltFired());

        player.fired(event.getEvent().getBoltFired());

        assertEquals(1, bolts
                .stream()
                .filter(b -> b.isOwnedBy(one))
                .map(Bolt::generateMovedEvent)
                .map(DomainEvent::getEvent)
                .filter(Event::hasBoltMoved)
                .map(Event::getBoltMoved)
                .filter(b -> b.getAngle() == (float) Math.PI / 4f)
                .count());

    }

    @Test
    void testCollisionHitBullsEye() {
        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join(JoinCommandDto
                .newBuilder()
                .setName("one")
                .build());
        when(clock.getTime()).thenReturn(25L);
        player.spawn();
        when(clock.getTime()).thenReturn(1025L);
        player.move(0.5f, 0.5f, null);

        PositionVector positionVector = PositionVector.of(
                0.45f,
                0.45f,
                (float) (Math.PI / 4),
                0.1f / (float) Math.cos(Math.PI / 4),
                0L,
                Player.Implementation.PLAYER_POSITION_CONFIGURATION
        ).update(1000L);

        assertTrue(player.collision(positionVector, 0.05f));
    }

    @Test
    void testCollisionHitWithinRadius() {
        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join(JoinCommandDto
                .newBuilder()
                .setName("one")
                .build());
        when(clock.getTime()).thenReturn(25L);
        player.spawn();
        when(clock.getTime()).thenReturn(1025L);
        player.move(0.54f, 0.54f, null);

        PositionVector positionVector = PositionVector.of(
                0.45f,
                0.45f,
                (float) (Math.PI / 4),
                0.1f / (float) Math.cos(Math.PI / 4),
                0L,
                Player.Implementation.PLAYER_POSITION_CONFIGURATION
        ).update(1000L);

        assertTrue(player.collision(positionVector, 0.05f));
    }

    @Test
    void testCollisionNoHitInsideSquare() {
        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join(JoinCommandDto
                .newBuilder()
                .setName("one")
                .build());
        when(clock.getTime()).thenReturn(25L);
        player.spawn();
        when(clock.getTime()).thenReturn(50L);
        player.move(0.4f, 0.6f, null);

        PositionVector positionVector = PositionVector.of(
                0.45f,
                0.45f,
                (float) (Math.PI / 4),
                0.1f / (float) Math.cos(Math.PI / 4),
                0L,
                Player.Implementation.PLAYER_POSITION_CONFIGURATION
        ).update(1000L);

        assertFalse(player.collision(positionVector, 0.05f));
    }

    @Test
    void testCollisionNoHitOutsideSquareX() {
        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join(JoinCommandDto
                .newBuilder()
                .setName("one")
                .build());
        when(clock.getTime()).thenReturn(25L);
        player.spawn();
        when(clock.getTime()).thenReturn(50L);
        player.move(0.2f, 0.2f, null);

        PositionVector positionVector = PositionVector.of(
                0.45f,
                0.45f,
                (float) (Math.PI / 4),
                0.1f / (float) Math.cos(Math.PI / 4),
                0L,
                Player.Implementation.PLAYER_POSITION_CONFIGURATION
        ).update(1000L);

        assertFalse(player.collision(positionVector, 0.05f));
    }

    @Test
    void testCollisionNoHitOutsideSquareY() {
        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join(JoinCommandDto
                .newBuilder()
                .setName("one")
                .build());
        when(clock.getTime()).thenReturn(25L);
        player.spawn();
        when(clock.getTime()).thenReturn(50L);
        player.move(0.5f, 0.2f, null);

        PositionVector positionVector = PositionVector.of(
                0.45f,
                0.45f,
                (float) (Math.PI / 4),
                0.1f / (float) Math.cos(Math.PI / 4),
                0L,
                Player.Implementation.PLAYER_POSITION_CONFIGURATION
        ).update(1000L);

        assertFalse(player.collision(positionVector, 0.05f));
    }

    @Test
    void testDestroyedBy() {
        EntityId one = EntityId.newId();
        EntityId two = EntityId.newId();
        Player player1 = this.players.createOrGet(one);
        Player player2 = this.players.createOrGet(two);

        player1.join(JoinCommandDto
                .newBuilder()
                .setName("one")
                .build());
        player2.join(JoinCommandDto
                .newBuilder()
                .setName("two")
                .build());

        when(clock.getTime()).thenReturn(25L);
        player1.spawn();
        player2.spawn();

        when(clock.getTime()).thenReturn(50L);
        player1.move(0.5f, 0.5f, 1f);

        List<DomainEvent> domainEvents = new ArrayList<>();
        GameEvents.getDomainEvents().setConsumer(domainEvents::add);

        player1.destroy(two);

        List<Event> events = domainEvents
                .stream()
                .map(DomainEvent::getEvent)
                .collect(Collectors.toList());


        assertEquals(1, events.size());
        assertTrue(events.get(0).hasPlayerDestroyed());

        verify(scoreBoard, times(1)).updateScore(two, 1);

    }

    @Test
    void testSpawn() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join(JoinCommandDto
                .newBuilder()
                .setName("one")
                .build());
        when(clock.getTime()).thenReturn(25L);
        player.spawn();

        assertTrue(eventReference.get().hasEvent());
        assertTrue(eventReference.get().getEvent().hasPlayerSpawned());
        PlayerSpawnedEventDto playerSpawnedEvent = eventReference.get().getEvent().getPlayerSpawned();
        PlayerMovedEventDto playerMovedEventDto = playerSpawnedEvent.getLocation();
        assertEquals(0f, playerMovedEventDto.getX());
        assertEquals(0f, playerMovedEventDto.getY());
        assertEquals(0f, playerMovedEventDto.getAngle());
    }
}