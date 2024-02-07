package pt.isel.gomoku.services

import kotlinx.datetime.Clock
import org.postgresql.util.PSQLException
import org.springframework.stereotype.Component
import pt.isel.gomoku.domain.token.Token
import pt.isel.gomoku.domain.token.TokenCreationError
import pt.isel.gomoku.domain.token.TokenCreationResult
import pt.isel.gomoku.domain.token.TokenExternalInfo
import pt.isel.gomoku.domain.users.Page
import pt.isel.gomoku.domain.users.RankingExternalInfo
import pt.isel.gomoku.domain.users.User
import pt.isel.gomoku.domain.users.UserCreationError
import pt.isel.gomoku.domain.users.UserCreationResult
import pt.isel.gomoku.domain.users.UserNotFound
import pt.isel.gomoku.domain.users.UserResult
import pt.isel.gomoku.domain.users.UserStatisticsResult
import pt.isel.gomoku.domain.users.UsersDomain
import pt.isel.gomoku.repository.TransactionManager
import pt.isel.gomoku.utils.failure
import pt.isel.gomoku.utils.success
import kotlin.math.roundToInt

/**
 * Represents different operations related to users.
 */
@Component
class UsersService(
    private val transactionManager: TransactionManager,
    private val usersDomain: UsersDomain,
    private val clock: Clock
) {

    /**
     * Creating a new user with given username, email and password.
     */
    fun createUser(username: String, email: String, password: String): UserCreationResult {
        // Cannot be created if password is unsafe.
        if (!usersDomain.isSafePassword(password)) return failure(UserCreationError.InsecurePassword)
        // Cannot be created if email provided is not in the right format.
        if (!usersDomain.canBeEmail(email)) return failure(UserCreationError.EmailNotValid)

        val passwordValidationInfo = usersDomain.createPasswordValidationInformation(password)

        return transactionManager.run {
            val usersRepository = it.usersRepository
            // Two users cannot have the same username or email.
            if (usersRepository.isUserStoredByUsernameOrEmail(username, email)) {
                failure(UserCreationError.UserAlreadyExists)
            } else {
                try {
                    val id = usersRepository.storeUser(username, email, passwordValidationInfo)
                    success(id)
                } catch (e: Exception) {
                    // NOTE: Due to the concurrency between validating whether the user does not exist and its creation,
                    //  the state of the database may change, causing a key duplication error (23505) ...
                    val cause = e.cause
                    if (cause is PSQLException && cause.sqlState == "23505") {
                        // ... so this is a failure with the UserAlreadyExists.
                        failure(UserCreationError.UserAlreadyExists)
                    } else {
                        throw e
                    }
                }
            }
        }
    }

    /**
     * Get the pagination ranking of users.
     */
    fun getUsersRanking(currentPage: Int): RankingExternalInfo {
        return transactionManager.run {
            // Use configuration limit.
            val limit = usersDomain.limitRanking

            // Calculate skip based on current page.
            val skip = currentPage.minus(1) * limit

            // Get the ranking of users, limited by limit. Can be paged through limit and skip.
            val listUsers = it.usersRepository.getUsersRanking(skip, limit)

            // Get the total number of users.
            val totalUsers = it.usersRepository.numberOfUsersInRanking() ?: 0

            // Calculate the last page of the ranking based on the number of users and limit.
            val lastPage = (totalUsers.toDouble() / limit).roundToInt()

            // Set the page corresponds to the ranking: ...
            val page = when {
                // ... First and unique page (all users represent in ranking).
                currentPage == 1 && totalUsers <= limit -> Page.Single
                // ... First page.
                currentPage == 1 -> Page.First(
                    next = currentPage + 1,
                    last = lastPage
                )
                // ... Last page.
                currentPage == lastPage -> Page.Last(
                    previous = currentPage - 1,
                    first = 1
                )
                // ... Middle page.
                else -> Page.Middle(
                    previous = currentPage - 1,
                    next = currentPage + 1,
                    first = 1,
                    last = lastPage
                )
            }
            RankingExternalInfo(totalUsers, listUsers, page)
        }
    }

    /**
     * Obtains the user with given ID.
     */
    fun getUserById(userId: Int): UserResult {
        return transactionManager.run {
            val usersRepository = it.usersRepository
            val user = usersRepository.getUserById(userId)
            // User with given ID does not exist.
            if (user == null) {
                failure(UserNotFound)
            } else {
                success(user)
            }
        }
    }

    /**
     * Obtains the statistics of user with given ID.
     */
    fun getUserStatistics(userId: Int): UserStatisticsResult {
        return transactionManager.run {
            val usersRepository = it.usersRepository
            val user = usersRepository.getUserById(userId)
            if (user == null) {
                failure(UserNotFound)
            } else {
                val statistics = it.usersRepository.getUserStatistics(user.id)
                checkNotNull(statistics)
                success(statistics)
            }
        }
    }

    /**
     * Generating a new token of user with given identifier (can be username or email).
     */
    fun createToken(identifier: String, password: String): TokenCreationResult {
        // Given information cannot be blank.
        if (identifier.isBlank() || password.isBlank()) {
            failure(TokenCreationError.InvalidCredentials)
        }

        return transactionManager.run {
            val usersRepository = it.usersRepository

            // User with given identification does not exist.
            val user = usersRepository.getUserByUsernameOrEmail(identifier)
                ?: return@run failure(TokenCreationError.InvalidCredentials)

            // Invalid if passwords don't match.
            if (!usersDomain.validatePassword(password, user.passwordValidation)) {
                if (!usersDomain.validatePassword(password, user.passwordValidation)) {
                    return@run failure(TokenCreationError.InvalidCredentials)
                }
            }

            // Generating a new token.
            val tokenValue = usersDomain.generateTokenValue()
            val now = clock.now()
            val newToken = Token(
                usersDomain.createTokenValidationInformation(tokenValue),
                user.id,
                createdAt = now,
                lastUsedAt = now
            )
            usersRepository.createToken(newToken, usersDomain.maxNumberOfTokensPerUser)
            success(
                TokenExternalInfo(
                    user.id,
                    user.username,
                    tokenValue,
                    usersDomain.getTokenExpiration(newToken)
                )
            )
        }
    }

    /**
     * Obtains the user with given token.
     */
    fun getUserByToken(token: String): User? {
        if (!usersDomain.canBeToken(token)) {
            return null
        }
        return transactionManager.run {
            val usersRepository = it.usersRepository
            val tokenValidationInfo = usersDomain.createTokenValidationInformation(token)
            val userAndToken = usersRepository.getTokenByTokenValidationInfo(tokenValidationInfo)
            if (userAndToken != null && usersDomain.isTokenTimeValid(clock, userAndToken.second)) {
                usersRepository.updateTokenLastUsed(userAndToken.second, clock.now())
                userAndToken.first
            } else {
                null
            }
        }
    }

    /**
     * Invalidates given token.
     */
    fun revokeToken(token: String): Boolean {
        val tokenValidationInfo = usersDomain.createTokenValidationInformation(token)
        return transactionManager.run {
            it.usersRepository.removeTokenByValidationInfo(tokenValidationInfo)
            true
        }
    }
}
