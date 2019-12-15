package me.paulo.casaes.bbop.model;

import me.paulo.casaes.bbop.dto.BoltMovedEventDto;
import me.paulo.casaes.bbop.dto.CommandType;
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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

class PlayerTest {

    @Mock
    private Clock clock;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        doReturn(0L).when(clock).getTime();

        Config.get().setEnv(Config.Environment.DEV.name());
        SingletonProvider.setClock(() -> clock);
        SingletonProvider.setPlayers(() -> Players.INSTANCE);

        Players.get().reset();
    }

    @Test
    void testCreate() {
        Player player = Players.get().createOrGet("1");

        assertTrue(StreamSupport.stream(Players.get().iterable().spliterator(), false)
                .anyMatch(p -> p == player));

        assertTrue(player.is("1"));
        assertFalse(player.is("2"));
    }

    @Test
    void testJoin() {

        PlayerJoinedEventDto event = Players.get().createOrGet("1").join();

        assertNotNull(event);
        assertEquals(EventType.PLAYER_JOINED, event.getEvent());
        assertEquals("1", event.getPlayerId());
        assertEquals(0f, event.getX());
        assertEquals(0f, event.getY());
        assertEquals(0f, event.getY());
    }

    @Test
    void testLeave() {
        Player player = Players.get().createOrGet("1");
        PlayerLeftEventDto event = player.leave();

        assertNotNull(event);
        assertEquals(EventType.PLAYER_LEFT, event.getEvent());
        assertEquals("1", event.getPlayerId());

        assertFalse(StreamSupport.stream(Players.get().iterable().spliterator(), false)
                .anyMatch(p -> p == player));

    }

    @Test
    void testRequestListOfPlayers() {
        Players.get().createOrGet("1").join();
        Players.get().createOrGet("2").join();

        PlayersListCommandDto command = Players.get().requestListOfPlayers();

        assertNotNull(command);
        assertEquals(CommandType.LIST_PLAYERS, command.getCommand());
        assertEquals(2, command.getPlayers().size());

        Set<String> playerIds = command.getPlayers()
                .stream()
                .map(PlayerDto::getPlayerId)
                .collect(Collectors.toCollection(HashSet::new));

        assertTrue(playerIds.contains("1"));
        assertTrue(playerIds.contains("2"));
    }

    @Test
    void testMove() {
        Player player = Players.get().createOrGet("1");
        player.join();
        Optional<PlayerMovedEventDto> event = player.move(0.1f, 0.2f, (float) Math.PI)
                .map(ev -> (PlayerMovedEventDto) ev);

        assertNotNull(event);
        assertTrue(event.isPresent());
        event
                .ifPresent(ev -> {
                    assertEquals(0.1f, ev.getX());
                    assertEquals(0.2f, ev.getY());
                    assertEquals((float) Math.PI, ev.getAngle());
                });

    }

    @Test
    void testBoundedMove() {
        Player player = Players.get().createOrGet("1");
        player.join();
        Optional<PlayerMovedEventDto> event = player.move(-1f, 2f, null)
                .map(ev -> (PlayerMovedEventDto) ev);

        assertNotNull(event);
        assertTrue(event.isPresent());
        event
                .ifPresent(ev -> {
                    assertEquals(0f, ev.getX());
                    assertEquals(1f, ev.getY());
                    assertEquals(0f, ev.getAngle());
                });

    }

    @Test
    void testOnlyAngleMove() {
        Player player = Players.get().createOrGet("1");
        player.join();
        Optional<PlayerMovedEventDto> event = player.move(0f, 0f, (float) Math.PI)
                .map(ev -> (PlayerMovedEventDto) ev);

        assertNotNull(event);
        assertTrue(event.isPresent());
        event
                .ifPresent(ev -> {
                    assertEquals(0f, ev.getX());
                    assertEquals(0f, ev.getY());
                    assertEquals((float) Math.PI, ev.getAngle());
                });

    }

    @Test
    void testNoMove() {
        Player player = Players.get().createOrGet("1");
        player.join();
        Optional<PlayerMovedEventDto> event = player.move(0f, 0f, null)
                .map(ev -> (PlayerMovedEventDto) ev);


        assertNotNull(event);
        assertFalse(event.isPresent());
    }

    @Test
    void testMaxFire() {
        Config.get().setMaxBolts(2);

        Player player = Players.get().createOrGet("1");
        player.join();

        player.fire();
        player.fire();
        player.fire();


        assertEquals(2, StreamSupport.stream(player.getActiveBolts()
                        .spliterator(),
                false)
                .count());

        assertEquals(2, StreamSupport.stream(player.getActiveBolts()
                        .spliterator(),
                false)
                .filter(b -> b.isOwnedBy("1"))
                .count());

    }

    @Test
    void testFireDirection() {
        Config.get().setMaxBolts(2);

        Player player = Players.get().createOrGet("1");
        player.join();
        player.move(0.5f, 0.5f, (float) Math.PI);

        player.fire();

        assertEquals(1, StreamSupport.stream(player.getActiveBolts()
                        .spliterator(),
                false)
                .filter(b -> b.isOwnedBy("1"))
                .map(Bolt::toEvent)
                .map(b -> (BoltMovedEventDto) b)
                .filter(b -> b.getAngle() == (float) Math.PI)
                .count());

    }

    @Test
    void testCollisionHitBullseye() {
        Player player = Players.get().createOrGet("1");
        player.join();
        player.move(0.5f, 0.5f, null);

        assertTrue(player.collision(0.45f, 0.45f, 0.55f, 0.55f, 0.05f));
    }

    @Test
    void testCollisionHitWithinRadius() {
        Player player = Players.get().createOrGet("1");
        player.join();
        player.move(0.54f, 0.54f, null);

        assertTrue(player.collision(0.45f, 0.45f, 0.55f, 0.55f, 0.05f));
    }

    @Test
    void testCollisionNoHitInsideSquare() {
        Player player = Players.get().createOrGet("1");
        player.join();
        player.move(0.4f, 0.6f, null);

        assertFalse(player.collision(0.45f, 0.45f, 0.55f, 0.55f, 0.05f));
    }

    @Test
    void testCollisionNoHitOutsideSquareX() {
        Player player = Players.get().createOrGet("1");
        player.join();
        player.move(0.2f, 0.2f, null);

        assertFalse(player.collision(0.45f, 0.45f, 0.55f, 0.55f, 0.05f));
    }

    @Test
    void testCollisionNoHitOutsideSquareY() {
        Player player = Players.get().createOrGet("1");
        player.join();
        player.move(0.5f, 0.2f, null);

        assertFalse(player.collision(0.45f, 0.45f, 0.55f, 0.55f, 0.05f));
    }

    @Test
    void testDestroyedBy() {
        Player player1 = Players.get().createOrGet("1");
        player1.join();
        Player player2 = Players.get().createOrGet("2");
        player2.join();

        player1.move(0.5f, 0.5f, 1f);

        List<EventDto> events = player1.destroyedBy("2");

        assertEquals(2, events.size());
        assertEquals(EventType.PLAYER_DESTROYED, events.get(0).getEvent());

        assertEquals(EventType.PLAYER_MOVED, events.get(1).getEvent());

        PlayerMovedEventDto playerMovedEventDto = (PlayerMovedEventDto) events.get(1);
        assertEquals(0f, playerMovedEventDto.getX());
        assertEquals(0f, playerMovedEventDto.getY());
        assertEquals(0f, playerMovedEventDto.getAngle());

    }
}