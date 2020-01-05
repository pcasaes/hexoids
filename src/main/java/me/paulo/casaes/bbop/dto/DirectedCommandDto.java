package me.paulo.casaes.bbop.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DirectedCommandDto implements Dto {
    private final String playerId;
    private final CommandDto command;

    private DirectedCommandDto(String playerId, CommandDto command) {
        this.playerId = playerId;
        this.command = command;
    }

    public static DirectedCommandDto of(String playerId, CommandDto command) {
        return new DirectedCommandDto(playerId, command);
    }

    public String getPlayerId() {
        return playerId;
    }

    public CommandDto getCommand() {
        return command;
    }

    @Override
    @JsonIgnore
    public Dto.Type getDtoType() {
        return DtoType.DIRECTED_COMMAND_DTO;
    }

    public enum DtoType implements Dto.Type {
        DIRECTED_COMMAND_DTO;
    }

}
