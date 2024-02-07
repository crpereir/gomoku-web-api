package pt.isel.gomoku.domain.games

import kotlin.time.Duration

/**
 * Used to configure domain.
 * Configurations are mainly related with times.
 */
data class GamesDomainConfig(
    val waitingTimeInWaitingLobby: Duration,
    val playingRoundTime: Duration
) {
    init {
        require(waitingTimeInWaitingLobby.isPositive())
        require(playingRoundTime.isPositive())
    }
}
