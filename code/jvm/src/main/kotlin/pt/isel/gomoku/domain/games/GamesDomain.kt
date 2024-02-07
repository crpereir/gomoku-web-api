package pt.isel.gomoku.domain.games

import org.springframework.stereotype.Component

@Component
class GamesDomain(config: GamesDomainConfig) {

    val waitingTimeInWaitingLobby = config.waitingTimeInWaitingLobby

    val playingRoundTime = config.playingRoundTime

    fun userIsPlayerOfGame(userId: Int, game: Game): Boolean =
        userId == game.createdPlayer || userId == game.joinedPlayer

    fun gameIsOver(game: Game): Boolean {
        return game.state == Game.State.WINNER_B || game.state == Game.State.WINNER_W || game.state == Game.State.DRAW
    }
}
