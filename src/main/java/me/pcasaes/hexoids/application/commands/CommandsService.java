package me.pcasaes.hexoids.application.commands;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CommandsService {

    private final Fire fireCommand;
    private final JoinGame joinGameCommand;
    private final LeaveGame leaveGameCommand;
    private final Move moveCommand;
    private final Spawn spawn;

    public CommandsService(Fire fireCommand,
                           JoinGame joinGameCommand,
                           LeaveGame leaveGameCommand,
                           Move moveCommand,
                           Spawn spawn) {
        this.fireCommand = fireCommand;
        this.joinGameCommand = joinGameCommand;
        this.leaveGameCommand = leaveGameCommand;
        this.moveCommand = moveCommand;
        this.spawn = spawn;
    }

    public Fire getFireCommand() {
        return fireCommand;
    }

    public JoinGame getJoinGameCommand() {
        return joinGameCommand;
    }

    public LeaveGame getLeaveGameCommand() {
        return leaveGameCommand;
    }

    public Move getMoveCommand() {
        return moveCommand;
    }

    public Spawn getSpawn() {
        return spawn;
    }
}
