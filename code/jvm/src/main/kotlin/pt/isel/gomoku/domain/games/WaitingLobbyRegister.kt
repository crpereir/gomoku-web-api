package pt.isel.gomoku.domain.games

import kotlinx.datetime.Instant
import java.util.UUID

/**
 * Represents a user waiting for a game to start.
 * Used for queries and possible matchmaking.
 */
data class WaitingLobbyRegister(
    val id: UUID,
    val createdUserId: Int,
    val createdAt: Instant,
    val deadline: Instant,
    val variantId: Int,
    val gameId: UUID
)
