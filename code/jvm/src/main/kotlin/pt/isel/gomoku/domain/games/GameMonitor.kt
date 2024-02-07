package pt.isel.gomoku.domain.games

import java.util.UUID

/**
 * Represents information sent to user after start attempt.
 */
sealed class StatusMonitor {

    // Used if game has not started yet.
    object StillWaiting : StatusMonitor()

    // Used if game is started.
    data class Started(val gameId: UUID) : StatusMonitor()
}

/**
 * Represents if user is not waiting for any game, mainly used if user tries to give up in waiting when
 * they haven't been waiting for a game to start.
 */
object MonitorNotFound
