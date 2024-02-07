package pt.isel.gomoku.domain.games

import kotlin.time.Duration

data class GameExternalInfo(
    val gameState: Game.State,
    val playerB: Int,
    val playerW: Int,
    val playingRoundTime: Duration,
    val pieces: List<Piece>,
    val variantId: Int
)
