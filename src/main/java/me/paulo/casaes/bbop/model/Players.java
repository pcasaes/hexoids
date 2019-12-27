package me.paulo.casaes.bbop.model;

import me.paulo.casaes.bbop.dto.DirectedCommandDto;
import me.paulo.casaes.bbop.dto.EventType;
import me.paulo.casaes.bbop.dto.PlayerDestroyedEventDto;
import me.paulo.casaes.bbop.dto.PlayerJoinedEventDto;
import me.paulo.casaes.bbop.dto.PlayerMovedEventDto;
import me.paulo.casaes.bbop.dto.PlayersListCommandDto;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Players {

    public static final Players INSTANCE = new Players();

    private final Map<String, Player> playerMap = new HashMap<>();

    public static Players get() {
        return INSTANCE;
    }

    private Players() {
    }

    public Player createOrGet(String id) {
        return playerMap.computeIfAbsent(id, this::create);
    }

    public Optional<Player> get(String id) {
        return Optional.ofNullable(playerMap.get(id));
    }

    private Player create(String id) {
        GameEvents.getClientEvents().register(DirectedCommandDto.of(id, Players.get().requestListOfPlayers()));
        return Player.create(id);
    }

    public PlayersListCommandDto requestListOfPlayers() {
        return PlayersListCommandDto.of(playerMap
                .values()
                .stream()
                .map(Player::toDto)
                .collect(Collectors.toList()));
    }

    public Iterable<Player> iterable() {
        return playerMap.values();
    }

    void joined(PlayerJoinedEventDto event) {
        Player player = createOrGet(event.getPlayerId());
        player.joined(event);
    }

    void left(String playerId) {
        Player player = createOrGet(playerId);
        playerMap.remove(playerId);
        player.left();
    }

    public void consumeFromJoinTopic(DomainEvent domainEvent) {
        if (domainEvent.getEvent() == null) {
            left(domainEvent.getKey());
        } else if (domainEvent.getEvent().isEvent(EventType.PLAYER_JOINED)) {
            joined((PlayerJoinedEventDto) domainEvent.getEvent());
        }
    }

    public void consumeFromPlayerActionTopic(DomainEvent domainEvent) {
        if (domainEvent.getEvent() != null && domainEvent.getEvent().isEvent(EventType.PLAYER_MOVED)) {
            get(domainEvent.getKey())
                    .ifPresent(p -> p.moved((PlayerMovedEventDto) domainEvent.getEvent()));
        }
        if (domainEvent.getEvent() != null && domainEvent.getEvent().isEvent(EventType.PLAYER_DESTROYED)) {
            get(domainEvent.getKey())
                    .ifPresent(p -> p.destroyed((PlayerDestroyedEventDto) domainEvent.getEvent()));
        }
    }

    void reset() {
        playerMap.clear();
    }

}
