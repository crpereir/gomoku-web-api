package pt.isel.gomoku.repository

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.gomoku.domain.games.Board
import pt.isel.gomoku.domain.games.Game.State.NEXT_TURN_W
import pt.isel.gomoku.domain.games.GamesDomain
import pt.isel.gomoku.domain.games.GamesDomainConfig
import pt.isel.gomoku.domain.games.Intersection
import pt.isel.gomoku.domain.games.OpeningRules
import pt.isel.gomoku.domain.games.Piece
import pt.isel.gomoku.domain.games.Player
import pt.isel.gomoku.domain.games.PlayingRules
import pt.isel.gomoku.domain.token.Sha256TokenEncoder
import pt.isel.gomoku.domain.users.PasswordValidationInfo
import pt.isel.gomoku.domain.users.UsersDomain
import pt.isel.gomoku.domain.users.UsersDomainConfig
import pt.isel.gomoku.repository.jdbi.JdbiGamesRepository
import pt.isel.gomoku.repository.jdbi.JdbiUsersRepository
import pt.isel.gomoku.repository.jdbi.StartStatus
import pt.isel.gomoku.repository.jdbi.StartStatus.Create
import pt.isel.gomoku.repository.jdbi.configureWithAppRequirements
import pt.isel.gomoku.utils.TestClock
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class GamesRepositoryTests {

    /**
     * Utility function that creates a user and returns its identifier.
     */
    private fun createUserAndGenerateId(repo: UsersRepository): Int {
        val userName = UsersRepositoryTests.newTestUserName()
        val email = UsersRepositoryTests.newTestEmail(userName)
        val passwordValidationInfo = PasswordValidationInfo(UsersRepositoryTests.newTokenValidationData())
        return repo.storeUser(userName, email, passwordValidationInfo)
    }

    @Test
    fun `can get all variants, validate valid variant and invalidate invalid variant`() = runWithHandle { handle ->
        // given: a GamesRepository
        val gRepo = JdbiGamesRepository(handle)

        // when: get all variants
        val variants = gRepo.getVariants()

        // then: the obtaining is successful
        assertEquals(1, variants[0].id)
        assertEquals(15, variants[0].boardDim)
        assertEquals(2, variants[1].id)
        assertEquals(19, variants[1].boardDim)

        // when : check if a variant in list of variants is valid
        // then:
        assertTrue(gRepo.isVariantValid(1))

        // when: check if a variant that is not in the list of list is valid
        // then:
        assertFalse(gRepo.isVariantValid(1000))
    }

    @Test
    fun `can map correctly variant postgres table to variant class`() = runWithHandle { handle ->
        // given: a GamesRepository
        val gRepo = JdbiGamesRepository(handle)

        // when: get variant
        val variant = gRepo.getVariant(1)

        // then: the obtaining is successful
        assertIs<OpeningRules>(variant.openingRules)
        assertIs<PlayingRules>(variant.playingRules)
    }

    @Test
    fun `can matchmaking algorithm select pairs of users and start games with them`() = runWithHandle { handle ->
        // given: a UsersRepository
        val uRepo = JdbiUsersRepository(handle)

        // and: four users
        val userIdA = createUserAndGenerateId(uRepo)
        val userIdB = createUserAndGenerateId(uRepo)
        val userIdC = createUserAndGenerateId(uRepo)
        val userIdD = createUserAndGenerateId(uRepo)

        // and: a GamesRepository
        val gRepo = JdbiGamesRepository(handle)

        // and: a test clock
        val clock = TestClock()

        // when: userA and userB express a desire to start a game with different configurations
        val resultForUserA = gRepo.start(userIdA, 1, clock.now(), gamesDomain.waitingTimeInWaitingLobby, gamesConfig.playingRoundTime)
        val resultForUserB = gRepo.start(userIdB, 2, clock.now(), gamesDomain.waitingTimeInWaitingLobby, gamesConfig.playingRoundTime)

        // then:
        assertIs<StartStatus.Register>(resultForUserA)
        assertTrue(gRepo.isUserWaiting(userIdA))
        assertTrue(gRepo.isUserWaiting(userIdA, resultForUserA.id))

        assertIs<StartStatus.Register>(resultForUserB)
        assertTrue(gRepo.isUserWaiting(userIdB))
        assertTrue(gRepo.isUserWaiting(userIdB, resultForUserB.id))

        // when: userC and userD express a desire to start a game with different configurations but C same as A and D same as B
        val resultForUserC = gRepo.start(userIdC, 1, clock.now(), gamesDomain.waitingTimeInWaitingLobby, gamesConfig.playingRoundTime)
        val resultForUserD = gRepo.start(userIdD, 2, clock.now(), gamesDomain.waitingTimeInWaitingLobby, gamesConfig.playingRoundTime)

        // then:
        assertIs<StartStatus.Create>(resultForUserC)
        assertFalse(gRepo.isUserWaiting(userIdC))
        assertFalse(gRepo.isUserWaiting(userIdC, resultForUserC.id))

        assertIs<StartStatus.Create>(resultForUserD)
        assertFalse(gRepo.isUserWaiting(userIdD))
        assertFalse(gRepo.isUserWaiting(userIdD, resultForUserD.id))

        assertEquals(resultForUserC.id, gRepo.getById(resultForUserA.id)?.id)
        assertEquals(resultForUserD.id, gRepo.getById(resultForUserB.id)?.id)
    }

    @Test
    fun `game is updated correctly`() = runWithHandle { handle ->
        // given: a UsersRepository
        val uRepo = JdbiUsersRepository(handle)

        // and: two users
        val userIdA = createUserAndGenerateId(uRepo)
        val userIdB = createUserAndGenerateId(uRepo)

        // and: a GamesRepository
        val gRepo = JdbiGamesRepository(handle)

        // and: a testClock marking instant
        val clock = TestClock()

        // when: userA start a game
        gRepo.start(userIdA, DEFAULT_VARIANT, clock.now(), gamesDomain.waitingTimeInWaitingLobby, gamesConfig.playingRoundTime)

        // when: userB start a game
        clock.advance(10.seconds)
        val result = gRepo.start(userIdB, DEFAULT_VARIANT, clock.now(), gamesDomain.waitingTimeInWaitingLobby, gamesConfig.playingRoundTime)

        // then:
        assertTrue { result is Create }
        val gameId = (result as Create).id

        // when: updating game
        clock.advance(1.minutes)
        gRepo.updateGame(gameId, generateNewBoardWithPiece(), NEXT_TURN_W, clock.now(), gamesConfig.playingRoundTime)

        // then: this should store in database
        val game = gRepo.getById(gameId)
        assertNotNull(game)
        assertEquals(generateNewBoardWithPiece(), game.board)
        assertEquals(NEXT_TURN_W, game.state)
    }

    companion object {

        const val DEFAULT_VARIANT = 1

        private fun generateNewBoardWithPiece() =
            Board(
                listOf(
                    Piece(
                        Player.B,
                        Intersection(0, 0)
                    )
                )
            )

        private fun runWithHandle(block: (Handle) -> Unit) = jdbi.useTransaction<Exception>(block)
        private fun newTestUserName() = "user-${abs(Random.nextLong())}"
        private fun newTestEmail(username: String) = "$username@gmail.com"
        private fun newTokenValidationData() = "token-${abs(Random.nextLong())}"

        private val jdbi = Jdbi.create(
            PGSimpleDataSource().apply {
                setURL("jdbc:postgresql://localhost:5432/db?user=dbuser&password=changeit")
            }
        ).configureWithAppRequirements()

        private val passwordEncoder = BCryptPasswordEncoder()
        private val tokenEncoder = Sha256TokenEncoder()
        private val usersConfig = UsersDomainConfig(
            tokenSizeInBytes = 256 / 8,
            tokenTtl = 24.hours,
            tokenRollingTtl = 1.hours,
            maxTokensPerUser = 3,
            limitUsersRanking = 10
        )
        val usersDomain = UsersDomain(passwordEncoder, tokenEncoder, usersConfig)

        private val gamesConfig = GamesDomainConfig(
            waitingTimeInWaitingLobby = 1.hours,
            playingRoundTime = 5.minutes
        )
        val gamesDomain = GamesDomain(gamesConfig)
    }
}
