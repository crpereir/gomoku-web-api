package pt.isel.gomoku.services

import org.jdbi.v3.core.Jdbi
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.gomoku.domain.games.Game.State.NEXT_TURN_B
import pt.isel.gomoku.domain.games.Game.State.NEXT_TURN_W
import pt.isel.gomoku.domain.games.Game.State.WINNER_B
import pt.isel.gomoku.domain.games.Game.State.WINNER_W
import pt.isel.gomoku.domain.games.GameError.GamePlayError
import pt.isel.gomoku.domain.games.GameError.GamePlayError.NotYourTurn
import pt.isel.gomoku.domain.games.GameError.GamePlayError.PositionNotAvailable
import pt.isel.gomoku.domain.games.GameError.GamePlayError.PositionNotValid
import pt.isel.gomoku.domain.games.GameError.GetGameError.GameNotFound
import pt.isel.gomoku.domain.games.GameError.GetGameError.PlayerNotPartOfGame
import pt.isel.gomoku.domain.games.GamesDomain
import pt.isel.gomoku.domain.games.GamesDomainConfig
import pt.isel.gomoku.domain.games.OpeningRules
import pt.isel.gomoku.domain.games.PlayingRules
import pt.isel.gomoku.domain.games.StartResultError
import pt.isel.gomoku.domain.games.StatusMonitor
import pt.isel.gomoku.domain.games.VariantError
import pt.isel.gomoku.domain.token.Sha256TokenEncoder
import pt.isel.gomoku.domain.users.UsersDomain
import pt.isel.gomoku.domain.users.UsersDomainConfig
import pt.isel.gomoku.repository.jdbi.JdbiTransactionManager
import pt.isel.gomoku.repository.jdbi.StartStatus
import pt.isel.gomoku.repository.jdbi.configureWithAppRequirements
import pt.isel.gomoku.utils.Either
import pt.isel.gomoku.utils.Failure
import pt.isel.gomoku.utils.Success
import pt.isel.gomoku.utils.TestClock
import java.util.*
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class GamesServicesTests {

    @Test
    fun `can obtain all available variants`() {
        // given: a GamesServices
        val gamesService = createGamesService()

        // when: get all variants
        val variants = gamesService.variants()

        // then:
        assertEquals(1, variants[0].id)
        assertEquals(15, variants[0].boardDim)
        assertEquals(2, variants[1].id)
        assertEquals(19, variants[1].boardDim)
    }

    @Test
    fun `can't express a desire to start a game with an invalid variant`() {
        // given: a UsersServices
        val usersService = createUsersService()

        // when: create an userA
        val username = newTestUserName()
        val email = newTestEmail(username)
        val userId = (usersService.createUser(username, email, PASSWORD) as Success).value

        // and given: a games services
        val gamesService = createGamesService()

        // when: start a game with invalid variant
        val startResult = gamesService.start(userId, 10000)

        // then:
        when (startResult) {
            is Either.Left -> when (startResult.value) {
                is StartResultError.VariantInvalid -> startResult.value
                is StartResultError.PlayerAlreadyWaiting -> fail(startResult.value.toString())
            }
            is Either.Right -> fail(startResult.value.toString())
        }
    }

    @Test
    fun `can obtain a valid game variant by ID`() {
        // given: a GamesServices
        val gamesService = createGamesService()

        // and given: a valid variant ID
        val validVariantId = 1

        // when: get the variant by ID
        val variantResult = gamesService.variant(validVariantId)

        // then:
        when (variantResult) {
            is Either.Left -> fail(variantResult.value.toString())
            is Either.Right -> {
                val variant = variantResult.value
                assertEquals(15, variant.boardDim)
                assertEquals(OpeningRules.NONE, variant.openingRules)
                assertEquals(PlayingRules.FREESTYLE, variant.playingRules)
            }
        }
    }

    @Test
    fun `receives VariantInvalid error for an invalid variant ID`() {
        // given: a GamesServices
        val gamesService = createGamesService()

        // and given: an invalid variant ID
        val invalidVariantId = 10000

        // when: get the variant by an invalid ID
        val variantResult = gamesService.variant(invalidVariantId)

        // then:
        when (variantResult) {
            is Either.Left -> {
                assertTrue(variantResult.value is VariantError.VariantInvalid)
            }
            is Either.Right -> fail(variantResult.value.toString())
        }
    }

    @Test
    fun `can't an player observe and delete an monitor that not exists`() {
        // given: a UsersServices
        val usersService = createUsersService()

        // when: create a user
        val username = newTestUserName()
        val email = newTestEmail(username)
        val userId = (usersService.createUser(username, email, PASSWORD) as Success).value

        // and given: a GamesServices
        val gamesService = createGamesService()

        // when: userA monitors its status
        val statusMonitorResult = gamesService.statusMonitor(userId, UUID.randomUUID())

        // then:
        when (statusMonitorResult) {
            is Either.Left -> statusMonitorResult.value
            is Either.Right -> fail(statusMonitorResult.value.toString())
        }

        // when: user give up wanting to play
        val firstDeleteMonitorResult = gamesService.deleteMonitor(userId, UUID.randomUUID())

        // then:
        when (firstDeleteMonitorResult) {
            is Either.Left -> statusMonitorResult.value
            is Either.Right -> fail(statusMonitorResult.value.toString())
        }
    }

    @Test
    fun `can matchmaking algorithm select pairs of users and start games with them`() {
        // given: a UsersServices
        val usersService = createUsersService()

        // when: create an userA
        val usernameA = newTestUserName()
        val emailA = newTestEmail(usernameA)
        val userAId = (usersService.createUser(usernameA, emailA, PASSWORD) as Success).value

        // when: create an userB
        val usernameB = newTestUserName()
        val emailB = newTestEmail(usernameB)
        val userBId = (usersService.createUser(usernameB, emailB, PASSWORD) as Success).value

        // and given: a GamesServices
        val gamesService = createGamesService()

        // when: userA express a desire to start a game
        val startResultForA = gamesService.start(userAId, DEFAULT_VARIANT)

        // then:
        val uniqueId = when (startResultForA) {
            is Either.Left -> fail(startResultForA.value.toString())
            is Either.Right -> when (startResultForA.value) {
                is StartStatus.Create -> fail((startResultForA.value as StartStatus.Create).id.toString())
                is StartStatus.Register -> (startResultForA.value as StartStatus.Register).id
            }
        }

        // when: userA monitors its status
        val firstStatusMonitorResult = gamesService.statusMonitor(userAId, uniqueId)

        // then:
        when (firstStatusMonitorResult) {
            is Either.Left -> fail(firstStatusMonitorResult.value.toString())
            is Either.Right -> when (firstStatusMonitorResult.value) {
                is StatusMonitor.StillWaiting -> firstStatusMonitorResult.value
                is StatusMonitor.Started -> fail((firstStatusMonitorResult.value as StatusMonitor.Started).gameId.toString())
            }
        }

        // when: userB express a desire to start a game
        val startResultForB = gamesService.start(userBId, DEFAULT_VARIANT)

        // then:
        val gameId = when (startResultForB) {
            is Either.Left -> fail(startResultForB.value.toString())
            is Either.Right -> when (startResultForB.value) {
                is StartStatus.Create -> (startResultForB.value as StartStatus.Create).id
                is StartStatus.Register -> fail((startResultForB.value as StartStatus.Register).id.toString())
            }
        }

        // when: userA monitors its status again
        val secondStatusMonitorResult = gamesService.statusMonitor(userAId, uniqueId)

        // then:
        when (secondStatusMonitorResult) {
            is Either.Left -> fail(secondStatusMonitorResult.value.toString())
            is Either.Right -> when (secondStatusMonitorResult.value) {
                is StatusMonitor.StillWaiting -> fail(secondStatusMonitorResult.value.toString())
                is StatusMonitor.Started -> (secondStatusMonitorResult.value as StatusMonitor.Started).gameId
            }
        }

        assertEquals(uniqueId, gameId)
    }

    @Test
    fun `can express a desire to start a game and give up`() {
        // given: a UsersServices
        val usersService = createUsersService()

        // when: create an user
        val username = newTestUserName()
        val email = newTestEmail(username)
        val userId = (usersService.createUser(username, email, PASSWORD) as Success).value

        // and given: a GamesServices
        val gamesService = createGamesService()

        // when: user express a desire to start a game
        val startResult = gamesService.start(userId, DEFAULT_VARIANT)

        // then:
        val uniqueId = when (startResult) {
            is Either.Left -> fail(startResult.value.toString())
            is Either.Right -> when (startResult.value) {
                is StartStatus.Create -> fail((startResult.value as StartStatus.Create).id.toString())
                is StartStatus.Register -> (startResult.value as StartStatus.Register).id
            }
        }

        // when: user monitors its status
        val statusMonitorResult = gamesService.statusMonitor(userId, uniqueId)

        // then:
        when (statusMonitorResult) {
            is Either.Left -> fail(statusMonitorResult.value.toString())
            is Either.Right -> when (statusMonitorResult.value) {
                is StatusMonitor.StillWaiting -> statusMonitorResult.value
                is StatusMonitor.Started -> fail((statusMonitorResult.value as StatusMonitor.Started).gameId.toString())
            }
        }

        // and: user give up wanting to play
        val deleteMonitorResult = gamesService.deleteMonitor(userId, uniqueId)

        // then:
        when (deleteMonitorResult) {
            is Either.Left -> fail(deleteMonitorResult.value.toString())
            is Either.Right -> deleteMonitorResult.value
        }
    }

    @Test
    fun `can't express the desire to start the game twice and can't find monitor status already deleted`() {
        // given: a UsersServices
        val usersService = createUsersService()

        // when: create an user
        val username = newTestUserName()
        val email = newTestEmail(username)
        val userId = (usersService.createUser(username, email, PASSWORD) as Success).value

        // and given: a GamesServices
        val gamesService = createGamesService()

        // when: user express a desire to start a game
        val fisrtStartResult = gamesService.start(userId, DEFAULT_VARIANT)

        // then:
        val uniqueId = when (fisrtStartResult) {
            is Either.Left -> fail(fisrtStartResult.value.toString())
            is Either.Right -> when (fisrtStartResult.value) {
                is StartStatus.Create -> fail((fisrtStartResult.value as StartStatus.Create).id.toString())
                is StartStatus.Register -> (fisrtStartResult.value as StartStatus.Register).id
            }
        }

        // when: user express a desire to start a game again
        val secondStartResult = gamesService.start(userId, DEFAULT_VARIANT)

        // then:
        when (secondStartResult) {
            is Either.Left -> secondStartResult.value.toString()
            is Either.Right -> when (secondStartResult.value) {
                is StartStatus.Create -> fail((secondStartResult.value as StartStatus.Create).id.toString())
                is StartStatus.Register -> fail((secondStartResult.value as StartStatus.Register).id.toString())
            }
        }

        // when: user monitors its status
        val firstStatusMonitorResult = gamesService.statusMonitor(userId, uniqueId)

        // then:
        when (firstStatusMonitorResult) {
            is Either.Left -> fail(firstStatusMonitorResult.value.toString())
            is Either.Right -> when (firstStatusMonitorResult.value) {
                is StatusMonitor.StillWaiting -> firstStatusMonitorResult.value
                is StatusMonitor.Started -> fail((firstStatusMonitorResult.value as StatusMonitor.Started).gameId.toString())
            }
        }

        // when: user give up wanting to play
        val firstDeleteMonitorResult = gamesService.deleteMonitor(userId, uniqueId)

        // then:
        when (firstDeleteMonitorResult) {
            is Either.Left -> fail(firstDeleteMonitorResult.value.toString())
            is Either.Right -> firstDeleteMonitorResult.value
        }

        // when: user give up again wanting to play
        val secondDeleteMonitorResult = gamesService.deleteMonitor(userId, uniqueId)

        // then:
        when (secondDeleteMonitorResult) {
            is Either.Left -> secondDeleteMonitorResult.value
            is Either.Right -> fail(secondDeleteMonitorResult.value.toString())
        }

        // when: user monitors its status
        val secondtatusMonitorResult = gamesService.statusMonitor(userId, uniqueId)

        // then:
        when (secondtatusMonitorResult) {
            is Either.Left -> secondtatusMonitorResult.value.toString()
            is Either.Right -> when (secondtatusMonitorResult.value) {
                is StatusMonitor.StillWaiting -> fail(secondtatusMonitorResult.value.toString())
                is StatusMonitor.Started -> fail((secondtatusMonitorResult.value as StatusMonitor.Started).gameId.toString())
            }
        }
    }

    @Test
    fun `after game is made, players can retrieve state`() {
        // given: a UsersServices
        val usersService = createUsersService()

        // and: two users (A and B)
        val usernameA = newTestUserName()
        val emailA = newTestEmail(usernameA)
        val userAId = (usersService.createUser(usernameA, emailA, PASSWORD) as Success).value

        val usernameB = newTestUserName()
        val emailB = newTestEmail(usernameB)
        val userBId = (usersService.createUser(usernameB, emailB, PASSWORD) as Success).value

        // and: a GamesServices
        val gamesService = createGamesService()

        // when: a game between user A ...
        val resultA = gamesService.start(userAId, DEFAULT_VARIANT)

        // then:
        assertTrue { resultA is Success }
        assertTrue { (resultA as Success).value is StartStatus.Register }

        // when: ... and user B
        val resultB = gamesService.start(userBId, DEFAULT_VARIANT)

        // then:
        assertTrue { resultB is Success }
        assertTrue { (resultB as Success).value is StartStatus.Create }

        val gameId = ((resultB as Success).value as StartStatus.Create).id

        // when: getting game
        val gameResult = gamesService.getById(userAId, gameId)

        // then: game should be in starting state
        assertTrue { gameResult is Success }
        val game = (gameResult as Success).value

        assertEquals(actual = emptyList(), expected = game.pieces)
        assertEquals(actual = NEXT_TURN_B, expected = game.gameState)
    }

    @Test
    fun `player retrieving game state that does not exist`() {
        // given: a UsersServices
        val usersService = createUsersService()

        // and: user A
        val usernameA = newTestUserName()
        val emailA = newTestEmail(usernameA)
        val userAId = (usersService.createUser(usernameA, emailA, PASSWORD) as Success).value

        // and: a GamesServices
        val gamesService = createGamesService()

        // when: getting game that does not exist
        val gameResult = gamesService.getById(userAId, UUID.randomUUID())

        // then: cause an error of it not existing
        assertTrue { gameResult is Failure }
        assertTrue { (gameResult as Failure).value is GameNotFound }
    }

    @Test
    fun `after game is made, user that are not players cannot retrieve state`() {
        // given: a UsersServices
        val usersService = createUsersService()

        // and: three users (A, B and C)
        val usernameA = newTestUserName()
        val emailA = newTestEmail(usernameA)
        val userAId = (usersService.createUser(usernameA, emailA, PASSWORD) as Success).value

        val usernameB = newTestUserName()
        val emailB = newTestEmail(usernameB)
        val userBId = (usersService.createUser(usernameB, emailB, PASSWORD) as Success).value

        val usernameC = newTestUserName()
        val emailC = newTestEmail(usernameC)
        val userCId = (usersService.createUser(usernameC, emailC, PASSWORD) as Success).value

        // and: a GamesServices
        val gamesService = createGamesService()

        // and: a game between users A and B
        val resultA = gamesService.start(userAId, DEFAULT_VARIANT)
        assertTrue { resultA is Success }
        assertTrue { (resultA as Success).value is StartStatus.Register }

        val resultB = gamesService.start(userBId, DEFAULT_VARIANT)
        assertTrue { resultB is Success }
        assertTrue { (resultB as Success).value is StartStatus.Create }

        val gameId = ((resultB as Success).value as StartStatus.Create).id

        // when: wrong user getting game state
        val gameResult = gamesService.getById(userCId, gameId)

        // then: cause an error of user not being player of game
        assertTrue { gameResult is Failure }
        assertTrue { (gameResult as Failure).value is PlayerNotPartOfGame }
    }

    @Test
    fun `can do successful play`() {
        // given: a UsersServices
        val usersService = createUsersService()

        // and: three users (A, B)
        val usernameA = newTestUserName()
        val emailA = newTestEmail(usernameA)
        val userAId = (usersService.createUser(usernameA, emailA, PASSWORD) as Success).value

        val usernameB = newTestUserName()
        val emailB = newTestEmail(usernameB)
        val userBId = (usersService.createUser(usernameB, emailB, PASSWORD) as Success).value

        // and: a GamesServices
        val gamesService = createGamesService()

        // and: a game between users A and B
        val resultA = gamesService.start(userAId, DEFAULT_VARIANT)
        assertTrue { resultA is Success }
        assertTrue { (resultA as Success).value is StartStatus.Register }

        val resultB = gamesService.start(userBId, DEFAULT_VARIANT)
        assertTrue { resultB is Success }
        assertTrue { (resultB as Success).value is StartStatus.Create }

        val gameId = ((resultB as Success).value as StartStatus.Create).id

        // given: a position to play
        val row = 0
        val column = 0

        // when: userA play
        val gameResult = gamesService.play(userAId, gameId, 0, 0)

        // then: new game is made
        assertTrue { gameResult is Success }
        val game = (gameResult as Success).value

        // and: the state transitioned
        assertTrue { game.gameState == NEXT_TURN_W }

        // and: the piece played is stored
        assertEquals(1, game.pieces.size)
        val playedPiece = game.pieces.first()
        assertTrue { playedPiece.intersection.row == row && playedPiece.intersection.column == column }
    }

    @Test
    fun `wrong player trying to play`() {
        // given: a UsersServices
        val usersService = createUsersService()

        // and: three users (A, B)
        val usernameA = newTestUserName()
        val emailA = newTestEmail(usernameA)
        val userAId = (usersService.createUser(usernameA, emailA, PASSWORD) as Success).value

        val usernameB = newTestUserName()
        val emailB = newTestEmail(usernameB)
        val userBId = (usersService.createUser(usernameB, emailB, PASSWORD) as Success).value

        // and: a GamesServices
        val gamesService = createGamesService()

        // and: a game between users A and B
        val resultA = gamesService.start(userAId, DEFAULT_VARIANT)
        assertTrue { resultA is Success }
        assertTrue { (resultA as Success).value is StartStatus.Register }

        val resultB = gamesService.start(userBId, DEFAULT_VARIANT)
        assertTrue { resultB is Success }
        assertTrue { (resultB as Success).value is StartStatus.Create }

        val gameId = ((resultB as Success).value as StartStatus.Create).id

        // when: userB play
        val gameResult = gamesService.play(userBId, gameId, 0, 0)

        // then: error occurred because it is not B's turn
        assertTrue { gameResult is Failure }
        assertTrue { (gameResult as Failure).value is NotYourTurn }
    }

    @Test
    fun `trying to play on invalid position`() {
        // given: a UsersServices
        val usersService = createUsersService()

        // and: three users (A, B)
        val usernameA = newTestUserName()
        val emailA = newTestEmail(usernameA)
        val userAId = (usersService.createUser(usernameA, emailA, PASSWORD) as Success).value

        val usernameB = newTestUserName()
        val emailB = newTestEmail(usernameB)
        val userBId = (usersService.createUser(usernameB, emailB, PASSWORD) as Success).value

        // and: a GamesServices
        val gamesService = createGamesService()

        // and: a game between users A and B
        val resultA = gamesService.start(userAId, DEFAULT_VARIANT)
        assertTrue { resultA is Success }
        assertTrue { (resultA as Success).value is StartStatus.Register }

        val resultB = gamesService.start(userBId, DEFAULT_VARIANT)
        assertTrue { resultB is Success }
        assertTrue { (resultB as Success).value is StartStatus.Create }

        val gameId = ((resultB as Success).value as StartStatus.Create).id

        // when: userA play
        val gameResult = gamesService.play(userAId, gameId, -1, 0)

        // then: returns error due to invalid position
        assertTrue { gameResult is Failure }
        assertTrue { (gameResult as Failure).value is PositionNotValid }
    }

    @Test
    fun `trying to play on occupied position`() {
        // given: UsersServices
        val usersService = createUsersService()

        // and: three users (A, B)
        val usernameA = newTestUserName()
        val emailA = newTestEmail(usernameA)
        val userAId = (usersService.createUser(usernameA, emailA, PASSWORD) as Success).value

        val usernameB = newTestUserName()
        val emailB = newTestEmail(usernameB)
        val userBId = (usersService.createUser(usernameB, emailB, PASSWORD) as Success).value

        // and: a GamesServices
        val gamesService = createGamesService()

        // and: a game between users A and B
        val resultA = gamesService.start(userAId, DEFAULT_VARIANT)
        assertTrue { resultA is Success }
        assertTrue { (resultA as Success).value is StartStatus.Register }

        val resultB = gamesService.start(userBId, DEFAULT_VARIANT)
        assertTrue { resultB is Success }
        assertTrue { (resultB as Success).value is StartStatus.Create }

        val gameId = ((resultB as Success).value as StartStatus.Create).id

        // when: userA play
        val gameResultA = gamesService.play(userAId, gameId, 0, 0)

        // then: game is stored
        assertTrue { gameResultA is Success }

        // when: userB play
        val gameResultB = gamesService.play(userBId, gameId, 0, 0)

        // then: game is stored
        assertTrue { gameResultB is Failure }
        assertTrue { (gameResultB as Failure).value is PositionNotAvailable }
    }

    @Test
    fun `trying to play after timeout`() {
        // given: a UsersServices
        val usersService = createUsersService()

        // and: three users (A, B)
        val usernameA = newTestUserName()
        val emailA = newTestEmail(usernameA)
        val userAId = (usersService.createUser(usernameA, emailA, PASSWORD) as Success).value

        val usernameB = newTestUserName()
        val emailB = newTestEmail(usernameB)
        val userBId = (usersService.createUser(usernameB, emailB, PASSWORD) as Success).value

        // and: a clock and GamesServices
        val clock = TestClock()
        val gamesService = createGamesService(clock)

        // and: a game between users A and B
        val resultA = gamesService.start(userAId, DEFAULT_VARIANT)
        assertTrue { resultA is Success }
        assertTrue { (resultA as Success).value is StartStatus.Register }

        val resultB = gamesService.start(userBId, DEFAULT_VARIANT)
        assertTrue { resultB is Success }
        assertTrue { (resultB as Success).value is StartStatus.Create }

        val gameId = ((resultB as Success).value as StartStatus.Create).id

        // and: fast-forward clock
        clock.advance(100.minutes)

        // when: userA play
        val gameResultA = gamesService.play(userAId, gameId, 0, 0)

        // then: returns error due to timeout
        assertTrue { gameResultA is Failure }
        assertTrue { (gameResultA as Failure).value is GamePlayError.TimeOut }
    }

    @Test
    fun `detecting win after winning play`() {
        // given: a UsersServices
        val usersService = createUsersService()

        // and: three users (A, B)
        val usernameA = newTestUserName()
        val emailA = newTestEmail(usernameA)
        val userAId = (usersService.createUser(usernameA, emailA, PASSWORD) as Success).value

        val usernameB = newTestUserName()
        val emailB = newTestEmail(usernameB)
        val userBId = (usersService.createUser(usernameB, emailB, PASSWORD) as Success).value

        // and: a GamesServices
        val gamesService = createGamesService()

        // and: a game between users A and B
        val resultA = gamesService.start(userAId, DEFAULT_VARIANT)
        assertTrue { resultA is Success }
        assertTrue { (resultA as Success).value is StartStatus.Register }

        val resultB = gamesService.start(userBId, DEFAULT_VARIANT)
        assertTrue { resultB is Success }
        assertTrue { (resultB as Success).value is StartStatus.Create }

        val gameId = ((resultB as Success).value as StartStatus.Create).id

        // 4 plays from each player to setup winning scenario
        repeat(4) { pos ->

            // when: userA play
            val gameResultA = gamesService.play(userAId, gameId, 0, pos)
            // then: game is stored
            assertTrue { gameResultA is Success }

            // when: userA play
            val gameResultB = gamesService.play(userBId, gameId, 1, pos)
            // then: game is stored
            assertTrue { gameResultB is Success }
        }

        // when: userA play
        val gameResultA = gamesService.play(userAId, gameId, 0, 4)

        // then: game is stored as completed via win
        assertTrue { gameResultA is Success }
        assertTrue { (gameResultA as Success).value.gameState == WINNER_B }
    }

    @Test
    fun `player giving up during game`() {
        // given: a UsersServices
        val usersService = createUsersService()

        // and: three users (A, B)
        val usernameA = newTestUserName()
        val emailA = newTestEmail(usernameA)
        val userAId = (usersService.createUser(usernameA, emailA, PASSWORD) as Success).value

        val usernameB = newTestUserName()
        val emailB = newTestEmail(usernameB)
        val userBId = (usersService.createUser(usernameB, emailB, PASSWORD) as Success).value

        // and: a GamesServices
        val gamesService = createGamesService()

        // and: a game between users A and B
        val resultA = gamesService.start(userAId, DEFAULT_VARIANT)
        assertTrue { resultA is Success }
        assertTrue { (resultA as Success).value is StartStatus.Register }

        val resultB = gamesService.start(userBId, DEFAULT_VARIANT)
        assertTrue { resultB is Success }
        assertTrue { (resultB as Success).value is StartStatus.Create }

        val gameId = ((resultB as Success).value as StartStatus.Create).id

        // when: give up on game
        val resultGiveUp = gamesService.giveUp(userAId, gameId)

        // then:
        assertTrue(resultGiveUp is Success)

        // when: observe game state
        val resultGame = gamesService.getById(userAId, gameId)
        assertTrue(resultGame is Success)

        // then:
        val game = resultGame.value
        assertEquals(WINNER_W, game.gameState)
    }

    companion object {
        private fun newTestUserName() = "user-${Math.abs(Random.nextLong())}"
        private fun newTestEmail(username: String) = "$username@gmail.com"

        private const val PASSWORD = "Random123"
        private const val DEFAULT_VARIANT = 1

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

        private val gamesConfig = GamesDomainConfig(
            waitingTimeInWaitingLobby = 1.hours,
            playingRoundTime = 5.minutes
        )
        val gamesDomain = GamesDomain(gamesConfig)

        private fun createGamesService(testClock: TestClock = TestClock()) = GamesService(
            JdbiTransactionManager(jdbi),
            gamesDomain,
            testClock
        )

        private val jdbi = Jdbi.create(
            PGSimpleDataSource().apply {
                setURL("jdbc:postgresql://localhost:5432/db?user=dbuser&password=changeit")
            }
        ).configureWithAppRequirements()
    }
}
