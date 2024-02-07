package pt.isel.gomoku.domain.games

/**
 * Represents different kinds of rules used when game is playing normally.
 */
enum class PlayingRules {

    /**
     * No special rules, only win with 5 in line.
     */
    FREESTYLE {
        override fun checkRules(boardDim: Int, pieces: List<Piece>, newPiece: Piece): Boolean {
            return true
        }
    };

    /**
     * Validates if piece can be played in playing rules of variant.
     */
    abstract fun checkRules(boardDim: Int, pieces: List<Piece>, newPiece: Piece): Boolean
}
