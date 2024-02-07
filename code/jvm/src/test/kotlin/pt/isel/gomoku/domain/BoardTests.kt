package pt.isel.gomoku.domain

import org.junit.jupiter.api.Test
import pt.isel.gomoku.domain.games.Board
import pt.isel.gomoku.domain.games.GameError
import pt.isel.gomoku.domain.games.Intersection
import pt.isel.gomoku.domain.games.Piece
import pt.isel.gomoku.domain.games.Player
import pt.isel.gomoku.domain.games.Variant
import pt.isel.gomoku.utils.Failure
import pt.isel.gomoku.utils.Success
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BoardTests {

    @Test
    fun `check for a draw in an empty board`() {
        // Arrange
        val board = Board.initial

        // Act
        // Assert
        assertFalse(board.isDraw(BOARD_DIM))
    }

    @Test
    fun `check for a win on an empty board`() {
        // Arrange
        val board = Board.initial

        // Act
        // Assert
        assertFalse(board.isWin(BOARD_DIM, Player.B))
        assertFalse(board.isWin(BOARD_DIM, Player.W))
    }

    @Test
    fun `get player at a specific position`() {
        // Arrange
        val pieces = listOf(
            Piece(Player.B, Intersection(0, 0)),
            Piece(Player.W, Intersection(1, 1))
        )
        val board = Board(pieces)

        // Act
        // Assert
        assertEquals(Player.B, board[Intersection(0, 0)])
        assertEquals(Player.W, board[Intersection(1, 1)])
        assertNull(board[Intersection(2, 2)])
    }

    @Test
    fun `check if the game is a draw`() {
        // Arrange
        val pieces = List(BOARD_DIM) { row ->
            List(BOARD_DIM) { column ->
                Piece(Player.B, Intersection(row, column))
            }
        }.flatten()
        val board = Board(pieces)

        // Act
        // Assert
        assertTrue(board.isDraw(BOARD_DIM))
    }

    @Test
    fun `test play invalid position (lastIntersection null or not in intersectionsInLine)`() {
        // Arrange
        val pieces = listOf(
            Piece(Player.B, Intersection(0, 0)),
            Piece(Player.B, Intersection(0, 1)),
            Piece(Player.B, Intersection(0, 2)),
            Piece(Player.B, Intersection(0, 3)),
            Piece(Player.B, Intersection(0, 13)),
            Piece(Player.B, Intersection(0, 14))
        )
        val board = Board(pieces)

        // Act
        // Assert
        assertFalse(board.isWin(BOARD_DIM, Player.B))
    }

    @Test
    fun `check if a player wins horizontally`() {
        // Arrange
        val pieces = listOf(
            Piece(Player.B, Intersection(0, 0)),
            Piece(Player.B, Intersection(0, 1)),
            Piece(Player.B, Intersection(0, 2)),
            Piece(Player.B, Intersection(0, 3)),
            Piece(Player.B, Intersection(0, 4))
        )
        val board = Board(pieces)

        // Act
        // Assert
        assertTrue(board.isWin(BOARD_DIM, Player.B))
    }

    @Test
    fun `check if a player wins vertically`() {
        // Arrange
        val pieces = listOf(
            Piece(Player.B, Intersection(0, 0)),
            Piece(Player.B, Intersection(1, 0)),
            Piece(Player.B, Intersection(2, 0)),
            Piece(Player.B, Intersection(3, 0)),
            Piece(Player.B, Intersection(4, 0))
        )
        val board = Board(pieces)

        // Act
        // Assert
        assertTrue(board.isWin(BOARD_DIM, Player.B))
    }

    @Test
    fun `check if a player wins diagonally (backslash)`() {
        // Arrange
        val pieces = listOf(
            Piece(Player.B, Intersection(0, 0)),
            Piece(Player.B, Intersection(1, 1)),
            Piece(Player.B, Intersection(2, 2)),
            Piece(Player.B, Intersection(3, 3)),
            Piece(Player.B, Intersection(4, 4))
        )
        val board = Board(pieces)

        // Act
        // Assert
        assertTrue(board.isWin(BOARD_DIM, Player.B))
    }

    @Test
    fun `check if a player wins diagonally (slash)`() {
        // Arrange
        val pieces = listOf(
            Piece(Player.B, Intersection(0, 4)),
            Piece(Player.B, Intersection(1, 3)),
            Piece(Player.B, Intersection(2, 2)),
            Piece(Player.B, Intersection(3, 1)),
            Piece(Player.B, Intersection(4, 0))
        )
        val board = Board(pieces)

        // Act
        // Assert
        assertTrue(board.isWin(BOARD_DIM, Player.B))
    }

    @Test
    fun `check if no player wins`() {
        // Arrange
        val pieces = listOf(
            Piece(Player.B, Intersection(2, 0)),
            Piece(Player.W, Intersection(0, 1)),
            Piece(Player.B, Intersection(2, 0)),
            Piece(Player.W, Intersection(1, 1)),
            Piece(Player.B, Intersection(2, 0))
        )
        val board = Board(pieces)

        // Act
        // Assert
        assertFalse(board.isWin(BOARD_DIM, Player.B))
        assertFalse(board.isWin(BOARD_DIM, Player.W))
    }

    @Test
    fun `test play valid position`() {
        // Arrange
        val board = Board.initial
        val currentPlayer = Player.B
        val variant = Variant.standard

        // Act
        val result = board.play(0, 0, currentPlayer, variant)

        // Assert
        assertTrue(result is Success)
    }

    @Test
    fun `test play invalid position (position not valid)`() {
        // Arrange
        val board = Board.initial
        val currentPlayer = Player.B
        val variant = Variant.standard

        // Act
        val result = board.play(-1, -1, currentPlayer, variant)

        // Assert
        assertTrue(result is Failure)
        assertEquals(GameError.GamePlayError.PositionNotValid, result.value)
    }

    @Test
    fun `test play invalid position (position not available)`() {
        // Arrange
        val board = Board(
            listOf(
                Piece(Player.B, Intersection(0, 0))
            )
        )
        val currentPlayer = Player.B
        val variant = Variant.standard

        // Act
        val result = board.play(0, 0, currentPlayer, variant)

        // Assert
        assertTrue(result is Failure)
        assertEquals(GameError.GamePlayError.PositionNotAvailable, result.value)
    }

    companion object {
        const val BOARD_DIM = 15
    }
}
