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
        GameEvents.getDomainEvents().setConsumer(domainEvent -> {
            Topics.valueOf(domainEvent.getTopic()).consume(domainEvent);
        });


        when(clock.getTime()).thenReturn(0L);

        Config.get().setEnv(Config.Environment.DEV.name());
        Config.get().setPlayerMaxMove(1f);
        Config.get().setMinMove(0.000000001f);
        Config.get().setPlayerMaxAngleDivisor(0.5f);
    }

    @Test
    void testCreate() {
        UUID one = UUID.randomUUID();
        UUID two = UUID.randomUUID();
        Player player = this.players.createOrGet(one);

        assertTrue(this.players.stream()
                .anyMatch(p -> p == player));

        assertTrue(player.is(one));
        assertFalse(player.is(two));

        assertSame(player, this.players.get(one).orElse(null));
        assertEquals(player, this.players.get(one.toString()).orElse(null));

        assertNotSame(player, this.players.get(two).orElse(null));
        assertNotEquals(player, this.players.get(two.toString()).orElse(null));

    }

    @Test
    void testJoin() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        UUID one = UUID.randomUUID();
        this.players.createOrGet(one).join();
        PlayerJoinedEventDto event = (PlayerJoinedEventDto) eventReference.get();

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

        this.players.joined(
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
        List<Dto> dtos = new ArrayList<>();
        GameEvents.getClientEvents().setConsumer(dtos::add);

        UUID one = UUID.randomUUID();
        Player player = this.players.createOrGet(one);
        player.leave();

        List<EventDto> events = dtos
                .stream()
                .filter(dto -> dto.getDtoType() == EventDto.DtoType.EVENT_DTO)
                .map(dto -> (EventDto) dto)
                .collect(Collectors.toList());

        assertEquals(1, events.size());

        EventDto event = events.get(0);
        assertEquals(EventType.PLAYER_LEFT, event.getEvent());
        assertEquals(one.toString(), ((PlayerLeftEventDto) event).getPlayerId());

        assertFalse(this.players.stream()
                .anyMatch(p -> p == player));

    }

    @Test
    void testRequestListOfPlayers() {
        UUID one = UUID.randomUUID();
        UUID two = UUID.randomUUID();
        this.players.createOrGet(one).join();
        this.players.createOrGet(two).join();

        PlayersListCommandDto command = this.players.requestListOfPlayers();

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
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        UUID one = UUID.randomUUID();
        Player player = this.players.createOrGet(one);
        player.join();
        player.move(0.1f, 0.2f, (float) Math.PI, (float) Math.PI);

        assertEquals(EventDto.DtoType.EVENT_DTO, eventReference.get().getDtoType());
        assertEquals(EventType.PLAYER_MOVED, ((EventDto) eventReference.get()).getEvent());
        PlayerMovedEventDto event = (PlayerMovedEventDto) eventReference.get();

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

        UUID one = UUID.randomUUID();
        Player player = this.players.createOrGet(one);
        player.join();
        player.move(-1f, 2f, null, null);

        assertEquals(EventDto.DtoType.EVENT_DTO, eventReference.get().getDtoType());
        assertEquals(EventType.PLAYER_MOVED, ((EventDto) eventReference.get()).getEvent());
        PlayerMovedEventDto event = (PlayerMovedEventDto) eventReference.get();

        assertNotNull(event);
        assertEquals(0f, event.getX());
        assertEquals(1f, event.getY());
        assertEquals(0f, event.getAngle());

    }

    @Test
    void testOnlyAngleMove() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        UUID one = UUID.randomUUID();
        Player player = this.players.createOrGet(one);
        player.join();
        player.move(0f, 0f, (float) Math.PI, (float) Math.PI);

        assertEquals(EventDto.DtoType.EVENT_DTO, eventReference.get().getDtoType());
        assertEquals(EventType.PLAYER_MOVED, ((EventDto) eventReference.get()).getEvent());
        PlayerMovedEventDto event = (PlayerMovedEventDto) eventReference.get();

        assertNotNull(event);
        assertEquals(0f, event.getX());
        assertEquals(0f, event.getY());
        assertEquals((float) Math.PI, event.getAngle());

    }

    @Test
    void testNoMove() {
        UUID one = UUID.randomUUID();
        Player player = this.players.createOrGet(one);
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
        Player player = this.players.createOrGet(one);
        player.join();

        player.fire();
        player.fire();
        player.fire();


        assertEquals(2, player.getActiveBoltCount());
    }

    @Test
    void testBoltExhaustion() {
        Config.get().setMaxBolts(2);

        UUID one = UUID.randomUUID();
        Player player = this.players.createOrGet(one);
        player.join();

        player.fire();

        assertEquals(1, player.getActiveBoltCount());

        player.boltExhausted();

        assertEquals(0, player.getActiveBoltCount());
    }

    @Test
    void testFireDirection() {
        Config.get().setMaxBolts(2);

        UUID one = UUID.randomUUID();
        Player player = this.players.createOrGet(one);
        player.join();
        player.move(0.5f, 0.5f, (float) Math.PI, (float) Math.PI);

        AtomicReference<DomainEvent> eventReference = new AtomicReference<>(null);
        GameEvents.getDomainEvents().setConsumer(eventReference::set);

        player.fire();

        DomainEvent event = eventReference.get();
        assertNotNull(event);
        assertEquals(EventType.BOLT_FIRED, event.getEvent().getEvent());

        player.fired((BoltFiredEventDto) event.getEvent());

        assertEquals(1, bolts
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
        Player player = this.players.createOrGet(one);
        player.join();
        player.move(0.5f, 0.5f, null, null);

        assertTrue(player.collision(0.45f, 0.45f, 0.55f, 0.55f, 0.05f));
    }

    @Test
    void testCollisionHitWithinRadius() {
        UUID one = UUID.randomUUID();
        Player player = this.players.createOrGet(one);
        player.join();
        player.move(0.54f, 0.54f, null, null);

        assertTrue(player.collision(0.45f, 0.45f, 0.55f, 0.55f, 0.05f));
    }

    @Test
    void testCollisionNoHitInsideSquare() {
        UUID one = UUID.randomUUID();
        Player player = this.players.createOrGet(one);
        player.join();
        player.move(0.4f, 0.6f, null, null);

        assertFalse(player.collision(0.45f, 0.45f, 0.55f, 0.55f, 0.05f));
    }

    @Test
    void testCollisionNoHitOutsideSquareX() {
        UUID one = UUID.randomUUID();
        Player player = this.players.createOrGet(one);
        player.join();
        player.move(0.2f, 0.2f, null, null);

        assertFalse(player.collision(0.45f, 0.45f, 0.55f, 0.55f, 0.05f));
    }

    @Test
    void testCollisionNoHitOutsideSquareY() {
        UUID one = UUID.randomUUID();
        Player player = this.players.createOrGet(one);
        player.join();
        player.move(0.5f, 0.2f, null, null);

        assertFalse(player.collision(0.45f, 0.45f, 0.55f, 0.55f, 0.05f));
    }

    @Test
    void testDestroyedBy() {
        UUID one = UUID.randomUUID();
        UUID two = UUID.randomUUID();
        Player player1 = this.players.createOrGet(one);
        player1.join();
        Player player2 = this.players.createOrGet(two);
        player2.join();

        player1.move(0.5f, 0.5f, 1f, 1f);

        List<DomainEvent> domainEvents = new ArrayList<>();
        GameEvents.getDomainEvents().setConsumer(domainEvents::add);

        player1.destroy(two);

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