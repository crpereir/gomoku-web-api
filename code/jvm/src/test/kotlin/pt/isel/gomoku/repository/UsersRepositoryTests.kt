package pt.isel.gomoku.repository

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.gomoku.domain.token.Sha256TokenEncoder
import pt.isel.gomoku.domain.token.Token
import pt.isel.gomoku.domain.token.TokenValidationInfo
import pt.isel.gomoku.domain.users.PasswordValidationInfo
import pt.isel.gomoku.domain.users.Ranking
import pt.isel.gomoku.domain.users.User
import pt.isel.gomoku.domain.users.UsersDomain
import pt.isel.gomoku.domain.users.UsersDomainConfig
import pt.isel.gomoku.repository.jdbi.JdbiUsersRepository
import pt.isel.gomoku.repository.jdbi.configureWithAppRequirements
import pt.isel.gomoku.utils.TestClock
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class UsersRepositoryTests {

    @Test
    fun `can create and retrieve user`() = runWithHandle { handle ->
        // given: a UsersRepository
        val repo = JdbiUsersRepository(handle)

        // when: storing a user
        val userName = newTestUserName()
        val email = newTestEmail(userName)
        val passwordValidationInfo = PasswordValidationInfo(newTokenValidationData())
        repo.storeUser(userName, email, passwordValidationInfo)

        // and: retrieving a user
        val retrievedUser: User? = repo.getUserByUsernameOrEmail(userName)

        // then:
        assertNotNull(retrievedUser)
        assertEquals(userName, retrievedUser.username)
        assertEquals(passwordValidationInfo, retrievedUser.passwordValidation)
        assertTrue(retrievedUser.id >= 0)

        // when: asking if the user exists
        val isUserIsStored = repo.isUserStoredByUsernameOrEmail(userName, email)

        // then: response is true
        assertTrue(isUserIsStored)

        // when: asking if a different user exists
        val anotherUserIsStored = repo.isUserStoredByUsernameOrEmail("another-$userName", "another-$email")

        // then: response is false
        assertFalse(anotherUserIsStored)
    }

    @Test
    fun `can create and validate tokens`() = runWithHandle { handle ->
        // given: a UsersRepository
        val repo = JdbiUsersRepository(handle)
        // and: a test clock
        val clock = TestClock()

        // and: a createdUser
        val userName = newTestUserName()
        val email = newTestEmail(userName)
        val passwordValidationInfo = PasswordValidationInfo("not-valid")
        val userId = repo.storeUser(userName, email, passwordValidationInfo)

        // and: test TokenValidationInfo
        val testTokenValidationInfo = TokenValidationInfo(newTokenValidationData())

        // when: creating a token
        val tokenCreationInstant = clock.now()
        val token = Token(
            testTokenValidationInfo,
            userId,
            createdAt = tokenCreationInstant,
            lastUsedAt = tokenCreationInstant
        )
        repo.createToken(token, 1)

        // then: createToken does not throw errors
        // no exception

        // when: retrieving the token and associated user
        val userAndToken = repo.getTokenByTokenValidationInfo(testTokenValidationInfo)

        // then:
        val (user, retrievedToken) = userAndToken ?: fail("token and associated user must exist")

        // and: ...
        assertEquals(userName, user.username)
        assertEquals(testTokenValidationInfo.validationInfo, retrievedToken.tokenValidationInfo.validationInfo)
        assertEquals(tokenCreationInstant, retrievedToken.createdAt)

        // when: update lastUsedAt
        val updateInstant = clock.now().plus(20.minutes)
        repo.updateTokenLastUsed(token, updateInstant)

        // then:
        assertEquals(repo.getTokenByTokenValidationInfo(token.tokenValidationInfo)!!.second.lastUsedAt, updateInstant)

        // when: remove token
        repo.removeTokenByValidationInfo(token.tokenValidationInfo)

        // then
        assertNull(repo.getTokenByTokenValidationInfo(token.tokenValidationInfo))
    }

    @Test
    fun `can get users Ranking`() = runWithHandle { handle ->
        // given: a UsersRepository
        val repo = JdbiUsersRepository(handle)

        // when: asking for ranking
        // then:
        assertNotNull(repo.numberOfUsersInRanking())
        assertIs<Ranking>(repo.getUsersRanking(0, 10))
    }

    @Test
    fun `can't get by id or get statistics for a user that does not exist`() = runWithHandle { handle ->
        // given: a UsersRepository
        val repo = JdbiUsersRepository(handle)

        // and: any id
        val userId = 10000

        // when: get user by id
        val userResult = repo.getUserById(userId)

        // then: the creation is unsuccessful, returning user not found error
        assertEquals(null, userResult)

        // when: get user statistics
        val userStatisticsResult = repo.getUserStatistics(userId)

        // then: the creation is unsuccessful, returning user not found error
        assertEquals(null, userStatisticsResult)
    }

    @Test
    fun `can create user, get the user and get statistics`() = runWithHandle { handle ->
        // given: a UsersRepository
        val repo = JdbiUsersRepository(handle)

        // when: storing a user
        val userName = newTestUserName()
        val email = newTestEmail(userName)
        val passwordValidationInfo = PasswordValidationInfo(newTokenValidationData())
        val result = repo.storeUser(userName, email, passwordValidationInfo)

        // then: the creation is successful
        assertTrue(result >= 0)

        // when: get by id
        val userId = result
        val userResult = repo.getUserById(userId)

        // then: the obtaining is successful
        if (userResult != null) {
            val user = userResult
            assertEquals(user.id, userId)
            assertEquals(user.username, userName)
            assertEquals(user.email, email)
        }

        // when: get statistics
        val userStatisticsResult = repo.getUserStatistics(userId)

        // then: the obtaining is successful
        assertNotNull(userStatisticsResult)
        assertEquals(userStatisticsResult.userId, userId)
        assertEquals(userStatisticsResult.username, userName)
        assertTrue(userStatisticsResult.gamesPlayed == 0)
        assertTrue(userStatisticsResult.gamesWon == 0)
        assertTrue(userStatisticsResult.gamesLost == 0)
    }

    companion object {
        private fun runWithHandle(block: (Handle) -> Unit) = jdbi.useTransaction<Exception>(block)
        fun newTestUserName() = "user-${abs(Random.nextLong())}"
        fun newTestEmail(username: String) = "$username@gmail.com"
        fun newTokenValidationData() = "token-${abs(Random.nextLong())}"

        private val jdbi = Jdbi.create(
            PGSimpleDataSource().apply {
                setURL("jdbc:postgresql://localhost:5432/db?user=dbuser&password=changeit")
            }
        ).configureWithAppRequirements()

        private val passwordEncoder = BCryptPasswordEncoder()
        private val tokenEncoder = Sha256TokenEncoder()
        private val config = UsersDomainConfig(
            tokenSizeInBytes = 256 / 8,
            tokenTtl = 24.hours,
            tokenRollingTtl = 1.hours,
            maxTokensPerUser = 3,
            limitUsersRanking = 10
        )
        val usersDomain = UsersDomain(passwordEncoder, tokenEncoder, config)
    }
}
