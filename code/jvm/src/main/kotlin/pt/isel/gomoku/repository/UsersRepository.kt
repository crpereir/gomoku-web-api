package pt.isel.gomoku.repository

import kotlinx.datetime.Instant
import pt.isel.gomoku.domain.token.Token
import pt.isel.gomoku.domain.token.TokenValidationInfo
import pt.isel.gomoku.domain.users.PasswordValidationInfo
import pt.isel.gomoku.domain.users.Ranking
import pt.isel.gomoku.domain.users.User
import pt.isel.gomoku.domain.users.UserStatistics

/**
 * Represents database access related to user operations.
 */
interface UsersRepository {

    /**
     * Verifies if there is a user with given username and email.
     */
    fun isUserStoredByUsernameOrEmail(username: String, email: String): Boolean

    /**
     * Creates a user with given username, email, and password validation information.
     */
    fun storeUser(username: String, email: String, passwordValidation: PasswordValidationInfo): Int

    /**
     * Get the ranking of users, limited by limit. Can be paged through limit and skip.
     */
    fun getUsersRanking(skip: Int, limit: Int): Ranking

    /**
     * Get the number of users in ranking.
     */
    fun numberOfUsersInRanking(): Int?

    /**
     * Obtains the user with given ID.
     */
    fun getUserById(userId: Int): User?

    /**
     * Obtains the statistics of user with given ID.
     */
    fun getUserStatistics(userId: Int): UserStatistics?

    /**
     * Obtains the user with given identity, which can be either username or email.
     */
    fun getUserByUsernameOrEmail(identity: String): User?

    /**
     * Generate a new token and eliminates any other token beyond maxTokens.
     */
    fun createToken(token: Token, maxTokens: Int)

    /**
     * Stores when the token was last used.
     */
    fun updateTokenLastUsed(token: Token, now: Instant)

    /**
     * Obtains the user and respective token via a token validation info.
     */
    fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>?

    /**
     * Removes a token with given validation info.
     */
    fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int
}
