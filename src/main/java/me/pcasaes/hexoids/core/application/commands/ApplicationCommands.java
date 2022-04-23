package me.pcasaes.hexoids.core.application.commands;

import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue;

public class ApplicationCommands {

    private final Fire fireCommand;
    private final JoinGame joinGameCommand;
    private final LeaveGame leaveGameCommand;
    private final Move moveCommand;
    private final Spawn spawn;
    private final SetInertialDampenFactor setInertialDampenFactorCommand;

    private ApplicationCommands(GameQueue gameQueue) {
        this.fireCommand = new Fire(gameQueue);
        this.joinGameCommand = new JoinGame(gameQueue);
        this.leaveGameCommand = new LeaveGame(gameQueue);
        this.moveCommand = new Move(gameQueue);
        this.spawn = new Spawn(gameQueue);
        this.setInertialDampenFactorCommand = new SetInertialDampenFactor(gameQueue);
    }

    public static ApplicationCommands create(GameQueue gameQueue) {
        return new ApplicationCommands(gameQueue);
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

    public SetInertialDampenFactor getSetInertialDampenFactorCommand() {
        return setInertialDampenFactorCommand;
    }
}
