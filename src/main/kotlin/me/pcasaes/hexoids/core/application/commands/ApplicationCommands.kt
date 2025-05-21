package me.pcasaes.hexoids.core.application.commands

import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue

class ApplicationCommands private constructor(gameQueue: GameQueue) {

    companion object {
        fun create(gameQueue: GameQueue): ApplicationCommands {
            return ApplicationCommands(gameQueue)
        }
    }

    private val fireCommand: Fire = Fire(gameQueue)
    private val joinGameCommand: JoinGame = JoinGame(gameQueue)
    private val leaveGameCommand: LeaveGame = LeaveGame(gameQueue)
    private val moveCommand: Move = Move(gameQueue)
    private val spawn: Spawn = Spawn(gameQueue)
    private val setInertialDampenFactorCommand: SetInertialDampenFactor = SetInertialDampenFactor(gameQueue)

    fun getFireCommand(): Fire {
        return fireCommand
    }

    fun getJoinGameCommand(): JoinGame {
        return joinGameCommand
    }

    fun getLeaveGameCommand(): LeaveGame {
        return leaveGameCommand
    }

    fun getMoveCommand(): Move {
        return moveCommand
    }

    fun getSpawn(): Spawn {
        return spawn
    }

    fun getSetInertialDampenFactorCommand(): SetInertialDampenFactor {
        return setInertialDampenFactorCommand
    }


}
