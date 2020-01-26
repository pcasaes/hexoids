package me.pcasaes.bbop.model;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pcasaes.bbop.proto.DirectedCommand;
import pcasaes.bbop.proto.Dto;
import pcasaes.bbop.proto.Event;
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
        Config.get().setMinMove(0.000000001f);
        Config.get().setPlayerMaxAngleDivisor(0.5f);
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
        this.players.createOrGet(one).join();
        PlayerJoinedEventDto event = eventReference.get().getEvent().getPlayerJoined();

        assertNotNull(event);
        assertEquals(one.getGuid(), event.getPlayerId());
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
        this.players.createOrGet(one).join();
        this.players.createOrGet(two).join();

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
        player.join();
        when(clock.getTime()).thenReturn(25L);
        player.spawn();
        when(clock.getTime()).thenReturn(50L);
        player.move(0.1f, 0.2f, (float) Math.PI, (float) Math.PI);

        assertTrue(eventReference.get().hasEvent());
        assertTrue(eventReference.get().getEvent().hasPlayerMoved());
        PlayerMovedEventDto event = eventReference.get().getEvent().getPlayerMoved();

        assertNotNull(event);
        assertEquals(0.1f, event.getX());
        assertEquals(0.2f, event.getY());
        assertEquals((float) Math.PI, event.getAngle());
        assertEquals((float) Math.PI, event.getThrustAngle());

    }

    @Test
    void testBoundedMove() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join();
        when(clock.getTime()).thenReturn(25L);
        player.spawn();
        when(clock.getTime()).thenReturn(50L);
        player.move(-1f, 2f, null, null);

        assertTrue(eventReference.get().hasEvent());
        assertTrue(eventReference.get().getEvent().hasPlayerMoved());
        PlayerMovedEventDto event = eventReference.get().getEvent().getPlayerMoved();

        assertNotNull(event);
        assertEquals(0f, event.getX());
        assertEquals(1f, event.getY());
        assertEquals(0f, event.getAngle());

    }

    @Test
    void testOnlyAngleMove() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join();
        when(clock.getTime()).thenReturn(25L);
        player.spawn();
        when(clock.getTime()).thenReturn(50L);
        player.move(0f, 0f, (float) Math.PI, (float) Math.PI);

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
        player.join();
        when(clock.getTime()).thenReturn(25L);
        player.spawn();
        when(clock.getTime()).thenReturn(50L);

        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        player.move(0f, 0f, null, null);

        assertNull(eventReference.get());
    }

    @Test
    void testMaxFire() {
        Config.get().setMaxBolts(2);

        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join();
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
        player.join();
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

        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join();
        when(clock.getTime()).thenReturn(25L);
        player.spawn();
        when(clock.getTime()).thenReturn(50L);
        player.move(0.5f, 0.5f, (float) Math.PI, (float) Math.PI);

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
    void testCollisionHitBullsEye() {
        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join();
        when(clock.getTime()).thenReturn(25L);
        player.spawn();
        when(clock.getTime()).thenReturn(50L);
        player.move(0.5f, 0.5f, null, null);

        assertTrue(player.collision(0.45f, 0.45f, 0.55f, 0.55f, 0.05f));
    }

    @Test
    void testCollisionHitWithinRadius() {
        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join();
        when(clock.getTime()).thenReturn(25L);
        player.spawn();
        when(clock.getTime()).thenReturn(50L);
        player.move(0.54f, 0.54f, null, null);

        assertTrue(player.collision(0.45f, 0.45f, 0.55f, 0.55f, 0.05f));
    }

    @Test
    void testCollisionNoHitInsideSquare() {
        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join();
        when(clock.getTime()).thenReturn(25L);
        player.spawn();
        when(clock.getTime()).thenReturn(50L);
        player.move(0.4f, 0.6f, null, null);

        assertFalse(player.collision(0.45f, 0.45f, 0.55f, 0.55f, 0.05f));
    }

    @Test
    void testCollisionNoHitOutsideSquareX() {
        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join();
        when(clock.getTime()).thenReturn(25L);
        player.spawn();
        when(clock.getTime()).thenReturn(50L);
        player.move(0.2f, 0.2f, null, null);

        assertFalse(player.collision(0.45f, 0.45f, 0.55f, 0.55f, 0.05f));
    }

    @Test
    void testCollisionNoHitOutsideSquareY() {
        EntityId one = EntityId.newId();
        Player player = this.players.createOrGet(one);
        player.join();
        when(clock.getTime()).thenReturn(25L);
        player.spawn();
        when(clock.getTime()).thenReturn(50L);
        player.move(0.5f, 0.2f, null, null);

        assertFalse(player.collision(0.45f, 0.45f, 0.55f, 0.55f, 0.05f));
    }

    @Test
    void testDestroyedBy() {
        EntityId one = EntityId.newId();
        EntityId two = EntityId.newId();
        Player player1 = this.players.createOrGet(one);
        Player player2 = this.players.createOrGet(two);

        player1.join();
        player2.join();

        when(clock.getTime()).thenReturn(25L);
        player1.spawn();
        player2.spawn();

        when(clock.getTime()).thenReturn(50L);
        player1.move(0.5f, 0.5f, 1f, 1f);

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
        player.join();
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