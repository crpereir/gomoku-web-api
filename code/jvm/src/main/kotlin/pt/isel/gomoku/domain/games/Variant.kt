package pt.isel.gomoku.domain.games

/**
 * Represents a game variant.
 * It can be represented with the size of board, a set of [OpeningRules] and a set of [PlayingRules].
 */
data class Variant(
    val boardDim: Int,
    val openingRules: OpeningRules,
    val playingRules: PlayingRules
) {

    companion object {
        val standard = Variant(15, OpeningRules.NONE, PlayingRules.FREESTYLE)
    }
}
