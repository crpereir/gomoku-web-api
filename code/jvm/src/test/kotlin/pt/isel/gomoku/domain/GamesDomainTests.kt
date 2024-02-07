package pt.isel.gomoku.domain

import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pt.isel.gomoku.domain.games.Board
import pt.isel.gomoku.domain.games.Game
import pt.isel.gomoku.domain.games.GamesDomain
import pt.isel.gomoku.domain.games.GamesDomainConfig
import java.util.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class GamesDomainTests {

    @Test
    fun `check if player is a player of a game`() {
        // Arrange
        // Act
        // Asset
        assertTrue(gamesDomain.userIsPlayerOfGame(1, game))
    }

    @Test
    fun `check if player is not a player of a game`() {
        // Arrange
        // Act
        // Asset
        assertFalse(gamesDomain.userIsPlayerOfGame(0, game))
    }

    @Test
    fun `check if game is not over`() {
        // Arrange
        // Act
        // Asset
        assertFalse(gamesDomain.gameIsOver(game))
    }

    @Test
    fun `check if game is over`() {
        // Arrange
        val gameEnded = game.copy(state = Game.State.WINNER_B)
        // Act
        // Asset
        assertTrue(gamesDomain.gameIsOver(gameEnded))
    }

    companion object {
        private val gameId: UUID = UUID.randomUUID()
        private val initialBoard = Board.initial
        private val clock = Clock.System
        private val createdAt = clock.now()
        private val updatedAt = clock.now()
        private val deadline = clock.now()
        private const val CREATED_PLAYER = 1
        private const val JOINED_PLAYER = 2
        private const val VARIANT_ID = 3
        val game = Game(
            gameId,
            initialBoard,
            Game.State.NEXT_TURN_B,
            createdAt,
            updatedAt,
            deadline,
            CREATED_PLAYER,
            JOINED_PLAYER,
            VARIANT_ID
        )
    }

    private val gamesDomainConfig = GamesDomainConfig(
        waitingTimeInWaitingLobby = 1.hours,
        playingRoundTime = 5.minutes
    )
    private val gamesDomain = GamesDomain(gamesDomainConfig)
}
