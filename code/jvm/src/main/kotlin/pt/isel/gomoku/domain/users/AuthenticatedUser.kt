package pt.isel.gomoku.domain.users

/**
 * Represents a user with respective, authenticated token.
 */
class AuthenticatedUser(
    val user: User,
    val token: String
)
