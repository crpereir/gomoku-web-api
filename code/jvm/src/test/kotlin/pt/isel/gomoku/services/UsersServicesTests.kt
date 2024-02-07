package pt.isel.gomoku.services

import org.jdbi.v3.core.Jdbi
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.gomoku.domain.token.Sha256TokenEncoder
import pt.isel.gomoku.domain.token.TokenCreationError
import pt.isel.gomoku.domain.users.UserCreationError
import pt.isel.gomoku.domain.users.UserNotFound
import pt.isel.gomoku.domain.users.UsersDomain
import pt.isel.gomoku.domain.users.UsersDomainConfig
import pt.isel.gomoku.repository.jdbi.JdbiTransactionManager
import pt.isel.gomoku.repository.jdbi.configureWithAppRequirements
import pt.isel.gomoku.utils.Either
import pt.isel.gomoku.utils.TestClock
import java.util.*
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class UsersServicesTests {

    @Test
    fun `can create user, get the user and get statistics`() {
        // given: a user service
        val usersService = createUsersService()

        // when: creating user
        val username = newTestUserName()
        val email = newTestEmail(username)
        val password = "Random123"
        val createUserResult = usersService.createUser(username, email, password)

        // then: the creation is successful
        when (createUserResult) {
            is Either.Left -> fail("Unexpected $createUserResult")
            is Either.Right -> assertTrue(createUserResult.value >= 0)
        }

        // when: get by id
        val userId = createUserResult.value
        val UserResult = usersService.getUserById(userId)

        // then: the obtaining is successful
        when (UserResult) {
            is Either.Left -> fail("Unexpected $UserResult")
            is Either.Right -> assertTrue(
                UserResult.value.id == userId &&
                    UserResult.value.username == username &&
                    UserResult.value.email == email
            )
        }

        // when: get statistics
        val UserStatisticsResult = usersService.getUserStatistics(userId)

        // then: the obtaining is successful
        when (UserStatisticsResult) {
            is Either.Left -> fail("Unexpected $UserStatisticsResult")
            is Either.Right -> assertTrue(
                UserStatisticsResult.value.userId == userId &&
                    UserStatisticsResult.value.username == username &&
                    UserStatisticsResult.value.gamesPlayed == 0 &&
                    UserStatisticsResult.value.gamesWon == 0 &&
                    UserStatisticsResult.value.gamesLost == 0 &&
                    UserStatisticsResult.value.gamesTied == 0
            )
        }
    }

    @Test
    fun `can detect insecure passwords`() {
        // given: a user service
        val usersService = createUsersService()

        // when: creating user
        val username = newTestUserName()
        val email = newTestEmail(username)
        val password = "random"
        val createUserResult = usersService.createUser(username, email, password)

        // then: the creation is unsuccessful, returning insecure password error
        when (createUserResult) {
            is Either.Left -> assertTrue { createUserResult.value is UserCreationError.InsecurePassword }
            is Either.Right -> fail("Unexpected $createUserResult")
        }
    }

    @Test
    fun `can detect invalid emails`() {
        // given: a user service
        val usersService = createUsersService()

        // when: creating user
        val username = newTestUserName()
        val email = "whatever"
        val password = "Random123"
        val createUserResult = usersService.createUser(username, email, password)

        // then: the creation is unsuccessful, returning invalid email error
        when (createUserResult) {
            is Either.Left -> assertTrue { createUserResult.value is UserCreationError.EmailNotValid }
            is Either.Right -> fail("Unexpected $createUserResult")
        }
    }

    @Test
    fun `can detect already existing users`() {
        // given: a user service
        val usersService = createUsersService()

        // and: a user already present
        val username = newTestUserName()
        val email = newTestEmail(username)
        val password = "Random123"
        val createUserResult = usersService.createUser(username, email, password)

        when (createUserResult) {
            is Either.Left -> fail("Unexpected $createUserResult")
            is Either.Right -> assertTrue(createUserResult.value > 0)
        }

        // when: creating user
        val anotherCreateUserResult = usersService.createUser(username, email, password)

        // then: the creation is unsuccessful, returning user already exists error
        when (anotherCreateUserResult) {
            is Either.Left -> assertTrue { anotherCreateUserResult.value is UserCreationError.UserAlreadyExists }
            is Either.Right -> fail("Unexpected $createUserResult")
        }
    }

    @Test
    fun `can't get by id or get statistics for a user that does not exist`() {
        // given: a user service
        val usersService = createUsersService()

        // and: a user id not in system
        val userId = 10000

        // when: get user by id
        val UserResult = usersService.getUserById(userId)

        // then: the creation is unsuccessful, returning user not found error
        when (UserResult) {
            is Either.Left -> assertTrue { UserResult.value is UserNotFound }
            is Either.Right -> fail("Unexpected $UserResult")
        }

        // when: get user statistics
        val UserStatisticsResult = usersService.getUserStatistics(userId)

        // then: the creation is unsuccessful, returning user not found error
        when (UserStatisticsResult) {
            is Either.Left -> assertTrue { UserStatisticsResult.value is UserNotFound }
            is Either.Right -> fail("Unexpected $UserStatisticsResult")
        }
    }

    @Test
    fun `can get users ranking`() {
        // given: a user service
        val usersService = createUsersService()

        // when: get users ranking
        usersService.getUsersRanking(currentPage = 1)

        // then:
        // no exception
    }

    @Test
    fun `can use token during rolling period but not after absolute TTL`() {
        // given: a user service
        val testClock = TestClock()
        val tokenTtl = 90.minutes
        val tokenRollingTtl = 30.minutes
        val userService = createUsersService(testClock, tokenTtl, tokenRollingTtl)

        // when: creating a user
        val username = newTestUserName()
        val email = newTestEmail(username)
        val password = "Random123"
        val createUserResult = userService.createUser(username, email, password)

        // then: the creation is successful
        when (createUserResult) {
            is Either.Left -> fail("Unexpected $createUserResult")
            is Either.Right -> assertTrue(createUserResult.value > 0)
        }

        // when: creating a token
        val createTokenResult = userService.createToken(username, password)

        // then: the creation is successful
        val token = when (createTokenResult) {
            is Either.Left -> fail(createTokenResult.toString())
            is Either.Right -> createTokenResult.value.tokenValue
        }

        // when: retrieving the user after (rolling TTL - 1s) intervals
        val startInstant = testClock.now()
        while (true) {
            testClock.advance(tokenRollingTtl.minus(1.seconds))
            userService.getUserByToken(token) ?: break
        }

        // then: user is not found only after the absolute TTL has elapsed
        assertTrue((testClock.now() - startInstant) > tokenTtl)
    }

    @Test
    fun `can create user, token, and retrieve by token`() {
        // given: a user service
        val testClock = TestClock()
        val userService = createUsersService(testClock)

        // when: creating a user
        val username = newTestUserName()
        val email = newTestEmail(username)
        val password = "Random123"
        val createUserResult = userService.createUser(username, email, password)

        // then: the creation is successful
        when (createUserResult) {
            is Either.Left -> fail("Unexpected $createUserResult")
            is Either.Right -> assertTrue(createUserResult.value > 0)
        }

        // when: creating a token
        val createTokenResult = userService.createToken(email, password)

        // then: the creation is successful
        val token = when (createTokenResult) {
            is Either.Left -> fail(createTokenResult.toString())
            is Either.Right -> createTokenResult.value.tokenValue
        }

        // and: the token bytes have the expected length
        val tokenBytes = Base64.getUrlDecoder().decode(token)
        assertEquals(256 / 8, tokenBytes.size)

        // when: retrieving the user by token
        val user = userService.getUserByToken(token)

        // then: a user is found
        assertNotNull(user)

        // and: has the expected name
        assertEquals(username, user.username)
        assertEquals(email, user.email)

        // when: delete a token
        assertTrue(userService.revokeToken(createTokenResult.value.tokenValue))

        // and: retrieving the user by token
        val userAgain = userService.getUserByToken(token)

        // then: a user is not found
        assertNull(userAgain)
    }

    @Test
    fun `invalid scenarios form token creation`() {
        // given: a user service
        val testClock = TestClock()
        val userService = createUsersService(testClock)

        // when: creating a user
        val username = newTestUserName()
        val email = newTestEmail(username)
        val password = "Random123"
        userService.createUser(username, email, password)

        // then: try invalid scenarios
        assertIs<Either.Left<TokenCreationError.InvalidCredentials>>(userService.createToken(email, ""))
        assertIs<Either.Left<TokenCreationError.InvalidCredentials>>(userService.createToken("", password))
        assertIs<Either.Left<TokenCreationError.InvalidCredentials>>(userService.createToken("fake-identity", password))
        assertIs<Either.Left<TokenCreationError.InvalidCredentials>>(userService.createToken(email, "fake-$password"))
        assertNull(userService.getUserByToken("thisIsNotAToken"))
    }

    companion object {

        private fun createUsersService(
            testClock: TestClock = TestClock(),
            tokenTtl: Duration = 30.days,
            tokenRollingTtl: Duration = 30.minutes,
            maxTokensPerUser: Int = 3
        ) = UsersService(
            JdbiTransactionManager(jdbi),
            UsersDomain(
                BCryptPasswordEncoder(),
                Sha256TokenEncoder(),
                UsersDomainConfig(
                    tokenSizeInBytes = 256 / 8,
                    tokenTtl = tokenTtl,
                    tokenRollingTtl,
                    maxTokensPerUser = maxTokensPerUser,
                    limitUsersRanking = 10
                )
            ),
            testClock
        )

        private fun newTestUserName() = "user-${Math.abs(Random.nextLong())}"
        private fun newTestEmail(username: String) = "$username@gmail.com"

        private val jdbi = Jdbi.create(
            PGSimpleDataSource().apply {
                setURL("jdbc:postgresql://localhost:5432/db?user=dbuser&password=changeit")
            }
        ).configureWithAppRequirements()
    }
}
