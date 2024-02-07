package pt.isel.gomoku.domain.games

/**
 * Represents a piece in game, placed by [Player] in [Intersection].
 */
data class Piece(val player: Player, val intersection: Intersection)
