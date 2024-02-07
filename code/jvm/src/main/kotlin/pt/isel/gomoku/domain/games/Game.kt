package pt.isel.gomoku.domain.games

import kotlinx.datetime.Instant
import pt.isel.gomoku.domain.games.GameError.GamePlayError.GameAlreadyEnded
import pt.isel.gomoku.domain.games.GameError.GamePlayError.NotYourTurn
import pt.isel.gomoku.domain.games.GameError.GamePlayError.TimeOut
import pt.isel.gomoku.utils.Failure
import pt.isel.gomoku.utils.Success
import pt.isel.gomoku.utils.failure
import pt.isel.gomoku.utils.success
import java.util.UUID

/**
 * Container that represents state of game.
 * Concrete game rules are presented in [Board], while control over executing play is done here.
 */
data class Game(
    val id: UUID,
    val board: Board,
    val state: State,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deadline: Instant,
    val createdPlayer: Int,
    val joinedPlayer: Int,
    val variantId: Int
) {

    enum class State {
        NEXT_TURN_W, NEXT_TURN_B, WINNER_W, WINNER_B, DRAW
    }

    /**
     * Before trying to play, checks if its worth it. If game already ended, it is not worth it.
     */
    fun play(userId: Int, row: Int, column: Int, variant: Variant, now: Instant): GamePlayResult {
        return when (state) {
            State.WINNER_W -> failure(GameAlreadyEnded)
            State.WINNER_B -> failure(GameAlreadyEnded)
            State.DRAW -> failure(GameAlreadyEnded)
            else -> processPlay(userId, row, column, variant, now)
        }
    }

    private fun processPlay(userId: Int, row: Int, column: Int, variant: Variant, now: Instant): GamePlayResult {
        // It is not worth checking game rules if...

        // ... the one trying to play is not the current player...
        if (userId != getCurrentUser()) return failure(NotYourTurn)

        // ... and if the current player took too long.
        if (now > deadline) return failure(TimeOut)

        val currentPlayer = getCurrentPlayer()
        return when (val result = board.play(row, column, currentPlayer, variant)) {
            // This failure represents failure in game logic in board.play().
            is Failure -> failure(result.value)

            // Processing change in game state.
            is Success -> {
                val newBoard = result.value
                val newState = when {
                    newBoard.isWin(variant.boardDim, currentPlayer) -> getWinner(currentPlayer)
                    newBoard.isDraw(variant.boardDim) -> State.DRAW
                    else -> getNextPlayer(currentPlayer)
                }
                success(
                    copy(
                        board = newBoard,
                        state = newState
                    )
                )
            }
        }
    }

    /**
     * Used to link player type, [Player.B] and [Player.W],
     * to users IDs, createdPlayer and joinedPlayer, respectively.
     */
    private fun getCurrentUser() = when (state) {
        State.NEXT_TURN_B -> createdPlayer
        State.NEXT_TURN_W -> joinedPlayer
        else -> throw IllegalStateException("Current player invalid.")
    }

    /**
     * Used to link player type, [Player.B] and [Player.W],
     * to game state, [State.NEXT_TURN_B] and [State.NEXT_TURN_W], respectively.
     */
    private fun getCurrentPlayer() = when (state) {
        State.NEXT_TURN_B -> Player.B
        State.NEXT_TURN_W -> Player.W
        else -> throw IllegalStateException("Current user invalid.")
    }

    /**
     * Used to current player, [Player.B] and [Player.W],
     * to game state representing next player, [State.NEXT_TURN_W] and [State.NEXT_TURN_B], respectively.
     */
    private fun getNextPlayer(currentPlayer: Player) = when (currentPlayer) {
        Player.B -> State.NEXT_TURN_W
        Player.W -> State.NEXT_TURN_B
    }

    /**
     * Used to link player who made winning play, [Player.B] and [Player.W],
     * to game state representing win, [State.WINNER_B] and [State.WINNER_W], respectively.
     */
    private fun getWinner(currentPlayer: Player) = when (currentPlayer) {
        Player.B -> State.WINNER_B
        Player.W -> State.WINNER_W
    }

    /**
     * Used to link player who lost via forfeit, [Player.B] and [Player.W],
     * to game state representing win, [State.WINNER_W] and [State.WINNER_B], respectively.
     *
     * Forfeit can either be done due to timeout or willingly giving up.
     */
    fun getForfeitWinner() = when (getCurrentPlayer()) {
        Player.B -> State.WINNER_W
        Player.W -> State.WINNER_B
    }
}
