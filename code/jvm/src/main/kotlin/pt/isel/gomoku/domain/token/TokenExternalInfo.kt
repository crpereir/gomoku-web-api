package pt.isel.gomoku.domain.token

import kotlinx.datetime.Instant

data class TokenExternalInfo(
    val userId: Int,
    val username: String,
    val tokenValue: String,
    val tokenExpiration: Instant
)
