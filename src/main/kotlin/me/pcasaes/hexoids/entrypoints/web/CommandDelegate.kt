package me.pcasaes.hexoids.entrypoints.web

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import me.pcasaes.hexoids.core.application.commands.ApplicationCommands
import me.pcasaes.hexoids.core.domain.model.EntityId
import pcasaes.hexoids.proto.JoinCommandDto
import pcasaes.hexoids.proto.MoveCommandDto
import pcasaes.hexoids.proto.SetFixedInertialDampenFactorCommandDto

@ApplicationScoped
class CommandDelegate @Inject constructor(
    private val applicationCommands: ApplicationCommands
) {

    fun fire(userId: EntityId) {
        this.applicationCommands.getFireCommand().fire(userId)
    }

    fun leave(userId: EntityId) {
        this.applicationCommands.getLeaveGameCommand().leave(userId)
    }

    fun spawn(userId: EntityId) {
        this.applicationCommands.getSpawn().spawn(userId)
    }

    fun join(userId: EntityId, command: JoinCommandDto) {
        this.applicationCommands.getJoinGameCommand().join(userId, command)
    }

    fun move(userId: EntityId, command: MoveCommandDto) {
        this.applicationCommands.getMoveCommand().move(userId, command)
    }

    fun setFixedInertialDampenFactor(userId: EntityId, command: SetFixedInertialDampenFactorCommandDto) {
        this.applicationCommands.getSetInertialDampenFactorCommand().setFactor(userId, command)
    }
}
