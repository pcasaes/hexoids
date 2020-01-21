package me.pcasaes.bbop.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Collection;

@RegisterForReflection
public class PlayersListCommandDto implements CommandDto {

    private final Collection<PlayerDto> players;

    private PlayersListCommandDto(Collection<PlayerDto> players) {
        this.players = players;
    }

    public static PlayersListCommandDto of(Collection<PlayerDto> players) {
        return new PlayersListCommandDto(players);
    }

    public Collection<PlayerDto> getPlayers() {
        return players;
    }

    @Override
    public CommandType getCommand() {
        return CommandType.LIST_PLAYERS;
    }
}
