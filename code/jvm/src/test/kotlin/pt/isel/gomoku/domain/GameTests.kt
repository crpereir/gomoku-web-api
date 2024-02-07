package pt.isel.gomoku.domain

import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import pt.isel.gomoku.domain.games.Board
import pt.isel.gomoku.domain.games.Game
import pt.isel.gomoku.domain.games.GameError
import pt.isel.gomoku.domain.games.GameError.GamePlayError.GameAlreadyEnded
import pt.isel.gomoku.domain.games.Intersection
import pt.isel.gomoku.domain.games.Piece
import pt.isel.gomoku.domain.games.Player
import pt.isel.gomoku.domain.games.Variant
import pt.isel.gomoku.utils.Failure
import pt.isel.gomoku.utils.Success
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class GameTests {

    @Test
    fun validPlay() {
        // Arrange
        // Act
        val result = game.play(game.createdPlayer, 1, 1, Variant.standard, instantAhead)

        // Assert
        assertTrue(result is Success)
        assertTrue(result.value.state != Game.State.NEXT_TURN_B)
    }

    @Test
    fun invalidPlay() {
        // Arrange
        // Act
        val result = game.play(game.joinedPlayer, 1, 1, Variant.standard, instantAhead)

        // Assert
        assertTrue(result is Failure)
        assertEquals(result.value, GameError.GamePlayError.NotYourTurn)
    }

    @Test
    fun gameAlreadyEndedWinnerB() {
        // Arrange
        val endedGame = game.copy(state = Game.State.WINNER_B)

        // Act
        val result = endedGame.play(endedGame.joinedPlayer, 2, 2, Variant.standard, instantAhead)

        // Assert
        assertTrue(result is Failure)
        assertEquals(result.value, GameAlreadyEnded)
    }

    @Test
    fun gameAlreadyEndedWinnerW() {
        // Arrange
        val endedGame = game.copy(state = Game.State.WINNER_W)

        // Act
        val result = endedGame.play(endedGame.createdPlayer, 2, 2, Variant.standard, instantAhead)

        // Assert
        assertTrue(result is Failure)
        assertEquals(result.value, GameAlreadyEnded)
    }

    @Test
    fun gameAlreadyEndedDraw() {
        // Arrange
        val endedGame = game.copy(state = Game.State.DRAW)

        // Act
        val result = endedGame.play(endedGame.createdPlayer, 2, 2, Variant.standard, instantAhead)

        // Assert
        assertTrue(result is Failure)
        assertEquals(result.value, GameAlreadyEnded)
    }

    @Test
    fun currentPlayerWinsGameB() {
        // Arrange
        val board = Board(
            listOf(
                Piece(Player.B, Intersection(1, 1)),
                Piece(Player.B, Intersection(1, 2)),
                Piece(Player.B, Intersection(1, 3)),
                Piece(Player.B, Intersection(1, 4))
            )
        )
        val gameState = game.copy(board = board, state = Game.State.NEXT_TURN_B)

        // Act
        val result = gameState.play(game.createdPlayer, 1, 5, Variant.standard, instantAhead)

        // Assert
        assertTrue(result is Success)
        assertEquals(result.value.state, Game.State.WINNER_B)
    }

    @Test
    fun currentPlayerWinsGameW() {
        // Arrange
        val board = Board(
            listOf(
                Piece(Player.W, Intersection(1, 1)),
                Piece(Player.W, Intersection(1, 2)),
                Piece(Player.W, Intersection(1, 3)),
                Piece(Player.W, Intersection(1, 4))
            )
        )
        val gameState = game.copy(board = board, state = Game.State.NEXT_TURN_W)

        // Act
        val result = gameState.play(game.joinedPlayer, 1, 5, Variant.standard, instantAhead)

        // Assert
        assertTrue(result is Success)
        assertEquals(result.value.state, Game.State.WINNER_W)
    }

    @Test
    fun currentPlayerDoesNotWinGameW() {
        // Arrange
        val board = Board(
            listOf(
                Piece(Player.B, Intersection(1, 1)),
                Piece(Player.B, Intersection(1, 2)),
                Piece(Player.B, Intersection(1, 3)),
                Piece(Player.W, Intersection(1, 4)),
                Piece(Player.W, Intersection(2, 4)),
                Piece(Player.W, Intersection(3, 4))
            )
        )
        val gameState = game.copy(board = board, state = Game.State.NEXT_TURN_B)

        // Act
        val result = gameState.play(game.createdPlayer, 1, 5, Variant.standard, instantAhead)

        // Assert
        assertTrue(result is Success)
        assertEquals(result.value.state, Game.State.NEXT_TURN_W)
    }

    @Test
    fun currentPlayerDoesNotWinGameB() {
        // Arrange
        val board = Board(
            listOf(
                Piece(Player.W, Intersection(1, 1)),
                Piece(Player.W, Intersection(1, 2)),
                Piece(Player.W, Intersection(1, 3)),
                Piece(Player.B, Intersection(1, 4)),
                Piece(Player.B, Intersection(2, 4)),
                Piece(Player.B, Intersection(3, 4))
            )
        )
        val gameState = game.copy(board = board, state = Game.State.NEXT_TURN_W)

        // Act
        val result = gameState.play(game.joinedPlayer, 2, 6, Variant.standard, instantAhead)

        // Assert
        assertTrue(result is Success)
        assertEquals(result.value.state, Game.State.NEXT_TURN_B)
    }

    @Test
    fun currentPlayerWhenWinnerB() {
        // Arrange
        val gameState = game.copy(state = Game.State.WINNER_B)

        // Act
        val result = gameState.play(game.joinedPlayer, 1, 1, Variant.standard, instantAhead)

        // Assert
        assertTrue(result is Failure)
        assertEquals(result.value, GameAlreadyEnded)
    }

    @Test
    fun currentPlayerWhenWinnerW() {
        // Arrange
        val gameState = game.copy(state = Game.State.WINNER_W)

        // Act
        val result = gameState.play(game.createdPlayer, 1, 1, Variant.standard, instantAhead)

        // Assert
        assertTrue(result is Failure)
        assertEquals(result.value, GameAlreadyEnded)
    }

    @Test
    fun currentUserWhenWinnerB() {
        // Arrange
        val gameState = game.copy(state = Game.State.WINNER_B)

        // Act
        val result = gameState.play(game.joinedPlayer, 1, 1, Variant.standard, instantAhead)

        // Assert
        assertTrue(result is Failure)
        assertEquals(result.value, GameAlreadyEnded)
    }

    @Test
    fun currentUserWhenWinnerW() {
        // Arrange
        val gameState = game.copy(state = Game.State.WINNER_W)

        // Act
        val result = gameState.play(game.createdPlayer, 1, 1, Variant.standard, instantAhead)

        // Assert
        assertTrue(result is Failure)
        assertEquals(result.value, GameAlreadyEnded)
    }

    @Test
    fun testTimeoutWinnerPlayerB() {
        // Arrange
        val gameState = game.copy(state = Game.State.NEXT_TURN_B)

        // Act
        val result = gameState.getForfeitWinner()

        // Assert
        assertEquals(result, Game.State.WINNER_W)
    }

    @Test
    fun testTimeoutWinnerPlayerW() {
        // Arrange
        val gameState = game.copy(state = Game.State.NEXT_TURN_W)

        // Act
        val result = gameState.getForfeitWinner()

        // Assert
        assertEquals(result, Game.State.WINNER_B)
    }

    @Test
    fun testPlayInvalidState() {
        // Arrange
        val gameState = game.copy(state = Game.State.WINNER_W)

        // Act
        val result = gameState.play(game.createdPlayer, 1, 1, Variant.standard, instantAhead)

        // Assert
        assertTrue(result is Failure)
        assertEquals(result.value, GameAlreadyEnded)
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
        val instantAhead = clock.now().minus(5.minutes)
    }
}
