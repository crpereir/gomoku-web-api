package pt.isel.gomoku.http.model.output

import pt.isel.gomoku.domain.games.Game
import pt.isel.gomoku.domain.games.Piece

data class GameOutputModel(
    val gameState: Game.State,
    val playerB: Int,
    val playerW: Int,
    val playingRoundTime: String,
    val pieces: List<Piece>,
    val variantId: Int
)
