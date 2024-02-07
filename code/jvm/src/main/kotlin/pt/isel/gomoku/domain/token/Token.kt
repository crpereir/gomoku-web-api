package pt.isel.gomoku.domain.token

import kotlinx.datetime.Instant

/**
 * Represents a token of user, including information to validate it.
 */
class Token(
    val tokenValidationInfo: TokenValidationInfo,
    val userId: Int,
    val createdAt: Instant,
    val lastUsedAt: Instant
)
