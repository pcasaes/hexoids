package me.pcasaes.hexoids.entrypoints.web;

import me.pcasaes.hexoids.core.application.commands.ApplicationCommands;
import me.pcasaes.hexoids.core.domain.model.EntityId;
import org.eclipse.microprofile.metrics.annotation.Counted;
import pcasaes.hexoids.proto.JoinCommandDto;
import pcasaes.hexoids.proto.MoveCommandDto;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
class CommandDelegate {

    private final ApplicationCommands applicationCommands;

    @Inject
    CommandDelegate(ApplicationCommands applicationCommands) {
        this.applicationCommands = applicationCommands;
    }

    @Counted(
            name = "bolt-fire-command",
            tags = "layer=entry-point",
            absolute = true
    )
    void fire(EntityId userId) {
        this.applicationCommands.getFireCommand().fire(userId);
    }

    @Counted(
            name = "leave-command",
            tags = "layer=entry-point",
            absolute = true
    )
    void leave(EntityId userId) {
        this.applicationCommands.getLeaveGameCommand().leave(userId);
    }

    @Counted(
            name = "spawn-command",
            tags = "layer=entry-point",
            absolute = true
    )
    void spawn(EntityId userId) {
        this.applicationCommands.getSpawn().spawn(userId);
    }

    @Counted(
            name = "join-command",
            tags = "layer=entry-point",
            absolute = true
    )
    void join(EntityId userId, JoinCommandDto command) {
        this.applicationCommands.getJoinGameCommand().join(userId, command);
    }

    void move(EntityId userId, MoveCommandDto command) {
        this.applicationCommands.getMoveCommand().move(userId, command);
    }
}
