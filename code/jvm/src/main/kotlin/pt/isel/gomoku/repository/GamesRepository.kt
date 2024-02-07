package pt.isel.gomoku.repository

import kotlinx.datetime.Instant
import pt.isel.gomoku.domain.games.Board
import pt.isel.gomoku.domain.games.Game
import pt.isel.gomoku.domain.games.Variant
import pt.isel.gomoku.domain.games.Variants
import pt.isel.gomoku.repository.jdbi.StartStatus
import java.util.UUID
import kotlin.time.Duration

/**
 * Represents database access related to game operations.
 */
interface GamesRepository {

    /**
     * Checks if user with given ID is waiting.
     */
    fun isUserWaiting(userId: Int): Boolean

    /**
     * Checks if user with given ID is waiting for a specific game.
     */
    fun isUserWaiting(userId: Int, gameId: UUID): Boolean

    /**
     * Attempts to start a game. Can either result in wait or in game creation.
     */
    fun start(
        userId: Int,
        variantId: Int,
        currentTime: Instant,
        waitingTimeInWaitingLobby: Duration,
        playingRoundTime: Duration
    ): StartStatus

    /**
     * Deletes the game creation request.
     */
    fun deleteStatusMonitor(userId: Int, gameId: UUID)

    /**
     * Obtains the current state of game with given ID.
     */
    fun getById(gameId: UUID): Game?

    /**
     * Check if the game has already started.
     */
    fun isGamePresent(userId: Int, gameId: UUID): Boolean

    /**
     * Checks to see if there is a variant with given ID.
     */
    fun isVariantValid(variantId: Int): Boolean

    /**
     * Obtains all the known game variants.
     * NOTE: Pagination are not used due to a small amount of known variations. These should be used
     * if the number of variations increases.
     */
    fun getVariants(): Variants

    /**
     * Obtains the information of game variant with given ID.
     */
    fun getVariant(variantId: Int): Variant

    /**
     * Updates the game state.
     */
    fun updateGame(gameId: UUID, newBoard: Board, newState: Game.State, updateTime: Instant, playingRoundTime: Duration)
}
