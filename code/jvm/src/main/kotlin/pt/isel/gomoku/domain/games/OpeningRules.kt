package pt.isel.gomoku.domain.games

/**
 * Represents different kinds of rules used when game is starting.
 */
enum class OpeningRules {

    /**
     * No special opening rules.
     */
    NONE {
        override fun checkRules(boardDim: Int, pieces: List<Piece>, newPiece: Piece): Boolean {
            return true
        }
    };

    /**
     * Validates if piece can be played in opening rules of variant.
     */
    abstract fun checkRules(boardDim: Int, pieces: List<Piece>, newPiece: Piece): Boolean
}
