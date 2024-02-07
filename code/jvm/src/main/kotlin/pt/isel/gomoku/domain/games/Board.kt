package pt.isel.gomoku.domain.games

import pt.isel.gomoku.domain.games.GameError.GamePlayError.PositionNotAvailable
import pt.isel.gomoku.domain.games.GameError.GamePlayError.PositionNotValid
import pt.isel.gomoku.domain.games.Line.BACKSLASH
import pt.isel.gomoku.domain.games.Line.COLUMN
import pt.isel.gomoku.domain.games.Line.ROW
import pt.isel.gomoku.domain.games.Line.SLASH
import pt.isel.gomoku.utils.failure
import pt.isel.gomoku.utils.success

const val CONSECUTIVE_PIECES_TO_WIN = 5
const val OFFSET_TO_WIN = CONSECUTIVE_PIECES_TO_WIN - 1

/**
 * Class that represents the pieces played in board, along with standard game rules.
 */
data class Board(val pieces: List<Piece>) {

    // Used to see if position is occupied by piece of player.
    operator fun get(intersection: Intersection): Player? =
        pieces.find { piece -> piece.intersection == intersection }?.player

    // Used to create valid positions (is within variant's board size).
    private fun newIntersection(row: Int, column: Int, boardDim: Int): Intersection? =
        if (row in 0 until boardDim && column in 0 until boardDim) Intersection(row, column) else null

    private fun isStarting() = pieces.size < 2

    fun play(row: Int, column: Int, currentPlayer: Player, variant: Variant): BoardPlayResult {
        // Validates if piece can be played...
        val newIntersection = newIntersection(row, column, variant.boardDim) ?: return failure(PositionNotValid)
        if (this[newIntersection] != null) return failure(PositionNotAvailable)

        val newPiece = Piece(currentPlayer, newIntersection)

        // These validations depend on variant rules...
        // ... opening
        if (isStarting() && !variant.openingRules.checkRules(variant.boardDim, pieces, newPiece)) {
            return failure(PositionNotValid)
        }

        // ... and playing.
        if (!variant.playingRules.checkRules(variant.boardDim, pieces, newPiece)) {
            return failure(PositionNotValid)
        }

        // ... then inserts piece in pieces list.
        return success(copy(pieces = pieces + newPiece))
    }

    /**
     * Used to see if game is in draw state.
     * As draw state is determined by the board being completely filled, it is dependent on variant's board size.
     */
    fun isDraw(boardDim: Int): Boolean =
        pieces.size == boardDim * boardDim

    /**
     * Used to see if currentPlayer won the game after play.
     * Winning positions are also dependent on variation's board size.
     */
    fun isWin(boardDim: Int, currentPlayer: Player): Boolean {
        // To check win, it only needs to see pieces of currentPlayer-
        val intersectionsOfCurrentPlayer = pieces
            .filter { piece -> piece.player == currentPlayer }
            .map { piece -> piece.intersection }

        /*
         Winning "in line" means winning via 4 ways:
         - In row ( -- )
         - In column ( | )
         - In backslash ( \ )
         - In slash ( / )
         */
        intersectionsOfCurrentPlayer.forEach { intersection ->
            if (
                checkLine(boardDim, intersectionsOfCurrentPlayer, intersection, ROW) ||
                checkLine(boardDim, intersectionsOfCurrentPlayer, intersection, COLUMN) ||
                checkLine(boardDim, intersectionsOfCurrentPlayer, intersection, BACKSLASH) ||
                checkLine(boardDim, intersectionsOfCurrentPlayer, intersection, SLASH)
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Depending on line type, checks to see if there is CONSECUTIVE_PIECES_TO_WIN pieces in line.
     */
    private fun checkLine(
        boardDim: Int,
        intersectionsOfCurrentPlayer: List<Intersection>,
        firstIntersection: Intersection,
        line: Line
    ): Boolean {
        val intersectionsInLine = intersectionsOfCurrentPlayer.filter { intersection ->
            when (line) {
                ROW -> intersection.row == firstIntersection.row
                COLUMN -> intersection.column == firstIntersection.column
                BACKSLASH -> intersection.backslash == firstIntersection.backslash
                SLASH -> intersection.slash == firstIntersection.slash
            }
        }

        if (intersectionsInLine.size >= CONSECUTIVE_PIECES_TO_WIN) {
            val lastIntersection = getLastIntersection(boardDim, firstIntersection, line)
            if (lastIntersection == null || !intersectionsInLine.contains(lastIntersection)) {
                return false
            }

            val comparisonList = buildComparisonList(firstIntersection, line)
            if (intersectionsInLine.containsAll(comparisonList)) {
                return true
            }
        }
        return false
    }

    /**
     * Obtains intersection that should be present in order for win state to occur.
     * This depends on the type of line requested and the first intersection in the assumed line of 5.
     * @return The last intersection, or null if it goes past the board.
     */
    private fun getLastIntersection(boardDim: Int, firstIntersection: Intersection, line: Line): Intersection? {
        val row = firstIntersection.row
        val column = firstIntersection.column
        return when (line) {
            ROW -> newIntersection(row, column + OFFSET_TO_WIN, boardDim)
            COLUMN -> newIntersection(row + OFFSET_TO_WIN, column, boardDim)
            BACKSLASH -> newIntersection(row + OFFSET_TO_WIN, column + OFFSET_TO_WIN, boardDim)
            SLASH -> newIntersection(row + OFFSET_TO_WIN, column - OFFSET_TO_WIN, boardDim)
        }
    }

    /**
     * Obtains the list of intersections that results in win state. As this is called after [getLastIntersection],
     * it is assumed that 5 in line is possible, now dependent if the middle intersections are also present.
     */
    private fun buildComparisonList(
        firstIntersection: Intersection,
        line: Line
    ): List<Intersection> {
        val row = firstIntersection.row
        val column = firstIntersection.column
        return when (line) {
            ROW -> List(CONSECUTIVE_PIECES_TO_WIN) { Intersection(row, column + it) }
            COLUMN -> List(CONSECUTIVE_PIECES_TO_WIN) { Intersection(row + it, column) }
            BACKSLASH -> List(CONSECUTIVE_PIECES_TO_WIN) { Intersection(row + it, column + it) }
            SLASH -> List(CONSECUTIVE_PIECES_TO_WIN) { Intersection(row + it, column - it) }
        }
    }

    companion object {
        val initial = Board(listOf())
    }
}
