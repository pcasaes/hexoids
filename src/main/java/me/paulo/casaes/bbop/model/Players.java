package me.paulo.casaes.bbop.model;

import me.paulo.casaes.bbop.dto.DirectedCommandDto;
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
        GameEvents.get().register(DirectedCommandDto.of(id, Players.get().requestListOfPlayers()));
        return Player.create(id);
    }

    public PlayersListCommandDto requestListOfPlayers() {
        return PlayersListCommandDto.of(playerMap
                .values()
                .stream()
                .map(Player::toDto)
                .collect(Collectors.toList()));
    }

    void remove(String id) {
        playerMap.remove(id);
    }

    public Iterable<Player> iterable() {
        return playerMap.values();
    }


    void reset() {
        playerMap.clear();
    }

}
