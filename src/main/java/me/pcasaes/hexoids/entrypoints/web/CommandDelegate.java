package me.pcasaes.hexoids.entrypoints.web;

import me.pcasaes.hexoids.core.application.commands.ApplicationCommands;
import me.pcasaes.hexoids.core.domain.model.EntityId;
import pcasaes.hexoids.proto.JoinCommandDto;
import pcasaes.hexoids.proto.MoveCommandDto;
import pcasaes.hexoids.proto.SetFixedInertialDampenFactorCommandDto;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
class CommandDelegate {

    private final ApplicationCommands applicationCommands;

    @Inject
    CommandDelegate(ApplicationCommands applicationCommands) {
        this.applicationCommands = applicationCommands;
    }

    void fire(EntityId userId) {
        this.applicationCommands.getFireCommand().fire(userId);
    }

    void leave(EntityId userId) {
        this.applicationCommands.getLeaveGameCommand().leave(userId);
    }

    void spawn(EntityId userId) {
        this.applicationCommands.getSpawn().spawn(userId);
    }

    void join(EntityId userId, JoinCommandDto command) {
        this.applicationCommands.getJoinGameCommand().join(userId, command);
    }

    void move(EntityId userId, MoveCommandDto command) {
        this.applicationCommands.getMoveCommand().move(userId, command);
    }

    void setFixedInertialDampenFactor(EntityId userId, SetFixedInertialDampenFactorCommandDto command) {
        this.applicationCommands.getSetInertialDampenFactorCommand().setFactor(userId, command);
    }
}
