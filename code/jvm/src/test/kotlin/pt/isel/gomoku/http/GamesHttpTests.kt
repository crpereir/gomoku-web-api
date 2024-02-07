package pt.isel.gomoku.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient
import pt.isel.gomoku.domain.games.VariantInfo
import pt.isel.gomoku.http.infra.SirenModel
import pt.isel.gomoku.http.model.output.Problem
import pt.isel.gomoku.utils.createUser
import pt.isel.gomoku.utils.login
import pt.isel.gomoku.utils.matchmaking
import pt.isel.gomoku.utils.startGame
import pt.isel.gomoku.utils.waitingForGame
import java.util.UUID
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GamesHttpTests {

    // One of the very few places where we use property injection
    @LocalServerPort
    var port: Int = 0

    @Test
    fun `can obtain all available variants`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        // and: create an user and log in
        val username = createUser(client)
        val token = login(client, username)

        // when: get all variants
        // then: the response is a 200 with a proper body
        client.get().uri("/api/games/variants")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MEDIA_TYPE_SIREN)
            .expectBody()
            .jsonPath("$.properties.variants[*].boardDim")
            .value(
                Matchers.everyItem(
                    Matchers.oneOf(15, 19)
                )
            )
            .jsonPath("$.properties.variants[*].openingRules")
            .value(
                Matchers.everyItem(
                    Matchers.oneOf("NONE")
                )
            )
            .jsonPath("$.properties.variants[*].playingRules")
            .value(
                Matchers.everyItem(
                    Matchers.oneOf("FREESTYLE")
                )
            )
    }

    @Test
    fun `can't express a desire to start a game with an invalid variant`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        // and: create an user and log in
        val username = createUser(client)
        val token = login(client, username)

        // when: start a game with invalid variant
        // then: the response is a 400
        client.post().uri("/api/games")
            .header("Authorization", "Bearer $token")
            .bodyValue(mapOf("variantId" to 1000))
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MEDIA_TYPE_ERROR)
    }

    @Test
    fun `can obtain a variant by ID`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        // and: create an user and log in
        val username = createUser(client)
        val token = login(client, username)

        // and: create a valid variant ID
        val validVariantId = 1

        // when: get a variant by ID
        // then: the response is a 200 with a proper body
        client.get().uri("/api/games/variants/$validVariantId")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MEDIA_TYPE_SIREN)
    }

    @Test
    fun `can't obtain an invalid variant by ID`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        // and: create an user and log in
        val username = createUser(client)
        val token = login(client, username)

        // and: create an invalid variant ID
        val invalidVariantId = 1000

        // when: get an invalid variant by ID
        // then: the response is a 400 with an error message
        client.get().uri("/api/games/variants/$invalidVariantId")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MEDIA_TYPE_ERROR)
    }

    @Test
    fun `can't an unauthorized player express a desire to start a game, observe and delete status monitor`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        // when: start a game with an invalid token
        // then: the response is a 401
        client.post().uri("/api/games")
            .header("Authorization", "Bearer fake-token")
            .exchange()
            .expectStatus().isUnauthorized

        // when: observe monitor with an invalid token
        // then: the response is a 401
        client.get().uri("/api/games/notRelevant/monitor")
            .header("Authorization", "Bearer fake-token")
            .exchange()
            .expectStatus().isUnauthorized

        // when: delete monitor with an invalid token
        // then: the response is a 401
        client.delete().uri("/api/games/notRelevant/monitor")
            .header("Authorization", "Bearer fake-token")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `can't an player observe and delete an monitor that not exists`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        // and: create an user and log in
        val username = createUser(client)
        val token = login(client, username)

        // when: observe monitor that not exists
        // then: the response is a 404
        client.get().uri("/api/games/${UUID.randomUUID()}/monitor")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().contentType(MEDIA_TYPE_ERROR)

        // when: delete monitor that not exists
        // then: the response is a 404
        client.delete().uri("/api/games/${UUID.randomUUID()}/monitor")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().contentType(MEDIA_TYPE_ERROR)
    }

    @Test
    fun `can matchmaking algorithm select pairs of users and start games with them`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        // and: create an userA and log in
        val usernameA = createUser(client)
        val tokenA = login(client, usernameA)

        // and: create an userB and log in
        val usernameB = createUser(client)
        val tokenB = login(client, usernameB)

        // when: userA express a desire to start a game
        val statusMonitor = waitingForGame(client, tokenA)

        // then: userA could monitor its status
        val link = client.get().uri("$statusMonitor")
            .header("Authorization", "Bearer $tokenA")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MEDIA_TYPE_SIREN)
            .expectBody(SirenModel::class.java)
            .returnResult()
            .responseBody!!
            .links
            .first { link -> link.rel.any { it == "self" } }.href

        assertEquals(statusMonitor.toString(), link)

        // when: userB express a desire to start a game
        val gameId = startGame(client, tokenB)

        // then: userA monitors its status again
        val gameIdLink = client.get().uri("$statusMonitor")
            .header("Authorization", "Bearer $tokenA")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MEDIA_TYPE_SIREN)
            .expectBody(SirenModel::class.java)
            .returnResult()
            .responseBody!!
            .links
            .first { link -> link.rel.any { it == "self" } }.href

        assertEquals(gameId, gameIdLink.substringAfter("/api/games/"))
    }

    @Test
    fun `can express a desire to start a game and give up`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        // and: create a user and log in
        val username = createUser(client)
        val token = login(client, username)

        // when: user express a desire to start a game
        val statusMonitor = waitingForGame(client, token)

        // then: user can monitor its status
        val link = client.get().uri("$statusMonitor")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MEDIA_TYPE_SIREN)
            .expectBody(SirenModel::class.java)
            .returnResult()
            .responseBody!!
            .links
            .first { link -> link.rel.any { it == "self" } }.href

        assertEquals(statusMonitor.toString(), link)

        // when: user give up wanting to play
        // then:
        client.delete().uri("$statusMonitor")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `can't express the desire to start the game twice and can't find monitor status already deleted`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        // and: create a user and log in
        val username = createUser(client)
        val token = login(client, username)

        // and: user express a desire to start a game
        val statusMonitor = waitingForGame(client, token)

        // when: user express a desire to start a game again
        // then: the response is a 403
        client.post().uri("/api/games")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isForbidden
            .expectHeader().contentType(MEDIA_TYPE_ERROR)

        // and: user monitors its status
        val link = client.get().uri("$statusMonitor")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MEDIA_TYPE_SIREN)
            .expectBody(SirenModel::class.java)
            .returnResult()
            .responseBody!!
            .links
            .first { link -> link.rel.any { it == "self" } }.href

        assertEquals(statusMonitor.toString(), link)

        // when: user give up wanting to play
        // then: the response is a 200
        client.delete().uri("$statusMonitor")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk

        // when: user give up again wanting to play
        // then: the response is a 404
        client.delete().uri("$statusMonitor")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isNotFound

        // and: user monitors its status
        // then: the response is a 404
        client.get().uri("$statusMonitor")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `after game is made, players can retrieve state`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        // and: create an userA and log in
        val usernameA = createUser(client)
        val tokenA = login(client, usernameA)

        // and: create an userB and log in
        val usernameB = createUser(client)
        val tokenB = login(client, usernameB)

        // and: userA and userB start game
        val gameId = matchmaking(client, tokenA, tokenB)

        // when: userA check game state
        // then:
        val gameStateA = client.get().uri("/api/games/$gameId")
            .header("Authorization", "Bearer $tokenA")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MEDIA_TYPE_SIREN)
            .expectBody(SirenModel::class.java)
            .returnResult()
            .responseBody!!
            .properties

        // when: userB check game state
        // then: should see same game state
        val gameStateB = client.get().uri("/api/games/$gameId")
            .header("Authorization", "Bearer $tokenB")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MEDIA_TYPE_SIREN)
            .expectBody(SirenModel::class.java)
            .returnResult()
            .responseBody!!
            .properties

        assertEquals(gameStateA, gameStateB)
    }

    @Test
    fun `player retrieving game state that does not exist`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        // and: create an userA and log in
        val usernameA = createUser(client)
        val tokenA = login(client, usernameA)

        // when: userA tries to get game that does not exist
        // then: the response is a 404
        val randomId = UUID.randomUUID()
        val problem = client.get().uri("/api/games/$randomId")
            .header("Authorization", "Bearer $tokenA")
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().contentType(MEDIA_TYPE_ERROR)
            .expectBody(Problem::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(problem.type, Problem.Type.GAME_NOT_FOUND)
    }

    @Test
    fun `after game is made, user that are not players cannot retrieve state`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        // and: create an userA and log in
        val usernameA = createUser(client)
        val tokenA = login(client, usernameA)

        // and: create an userB and log in
        val usernameB = createUser(client)
        val tokenB = login(client, usernameB)

        // and: userA and userB start game
        val gameId = matchmaking(client, tokenA, tokenB)

        // and: create an userC and log in
        val usernameC = createUser(client)
        val tokenC = login(client, usernameC)

        // when: userC check game state
        // then: cannot
        val problem = client.get().uri("/api/games/$gameId")
            .header("Authorization", "Bearer $tokenC")
            .exchange()
            .expectStatus().isForbidden
            .expectHeader().contentType(MEDIA_TYPE_ERROR)
            .expectBody(Problem::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(problem.type, Problem.Type.PLAYER_NOT_PART_OF_GAME)
    }

    @Test
    fun `can create a game, make a move, and check if the move is in the game state`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        // and: create an userA and log in
        val usernameA = createUser(client)
        val tokenA = login(client, usernameA)

        // and: create an userB and log in
        val usernameB = createUser(client)
        val tokenB = login(client, usernameB)

        // and: userA and userB start game
        val gameId = matchmaking(client, tokenA, tokenB)

        // when: userA play a round
        // then: the response is a 200
        client.put().uri("/api/games/$gameId/play/row/1/column/1")
            .header("Authorization", "Bearer $tokenA")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MEDIA_TYPE_SIREN)

        // when: userA check game state
        // then:
        client.get().uri("/api/games/$gameId")
            .header("Authorization", "Bearer $tokenA")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MEDIA_TYPE_SIREN)
            .expectBody()
            .jsonPath("$.properties.pieces[0].intersection.row")
            .isEqualTo(1)
    }

    @Test
    fun `wrong player trying to play`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        // and: create an userA and log in
        val usernameA = createUser(client)
        val tokenA = login(client, usernameA)

        // and: create an userB and log in
        val usernameB = createUser(client)
        val tokenB = login(client, usernameB)

        // and: userA and userB start game
        val gameId = matchmaking(client, tokenA, tokenB)

        // when: userB play without taking your turn
        // then: the response is 400
        val problem = client.put().uri("/api/games/$gameId/play/row/1/column/1")
            .header("Authorization", "Bearer $tokenB")
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MEDIA_TYPE_ERROR)
            .expectBody(Problem::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(Problem.Type.NOT_YOUR_TURN, problem.type)
    }

    @Test
    fun `move not valid`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        // and: create an userA and log in
        val usernameA = createUser(client)
        val tokenA = login(client, usernameA)

        // and: create an userB and log in
        val usernameB = createUser(client)
        val tokenB = login(client, usernameB)

        // and: userA and userB start game
        val gameId = matchmaking(client, tokenA, tokenB)

        // when: userA tries to play in an invalid position
        // then: the response is 400
        val problem = client.put().uri("/api/games/$gameId/play/row/100000/column/1")
            .header("Authorization", "Bearer $tokenA")
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MEDIA_TYPE_ERROR)
            .expectBody(Problem::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(Problem.Type.MOVE_NOT_VALID, problem.type)
    }

    @Test
    fun `move not valid, a piece already exists in that place`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        // and: create an userA and log in
        val usernameA = createUser(client)
        val tokenA = login(client, usernameA)

        // and: create an userB and log in
        val usernameB = createUser(client)
        val tokenB = login(client, usernameB)

        // and: userA and userB start game
        val gameId = matchmaking(client, tokenA, tokenB)

        // and: userA makes a move
        client.put().uri("/api/games/$gameId/play/row/1/column/1")
            .header("Authorization", "Bearer $tokenA")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MEDIA_TYPE_SIREN)
            .expectBody(SirenModel::class.java)

        // when: userB plays in the same place
        // then: the response is 400
        val problem = client.put().uri("/api/games/$gameId/play/row/1/column/1")
            .header("Authorization", "Bearer $tokenB")
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MEDIA_TYPE_ERROR)
            .expectBody(Problem::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(Problem.Type.MOVE_NOT_VALID, problem.type)
    }

    @Test
    fun `game not found`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        // and: create an userA and log in
        val usernameA = createUser(client)
        val tokenA = login(client, usernameA)

        // and: a random id
        val gameId = UUID.randomUUID()

        // when: make a move in a game that doesn't exist
        // then: the response is 404
        val problem = client.put().uri("/api/games/$gameId/play/row/1/column/1")
            .header("Authorization", "Bearer $tokenA")
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().contentType(MEDIA_TYPE_ERROR)
            .expectBody(Problem::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(Problem.Type.GAME_NOT_FOUND, problem.type)
    }

    @Test
    fun `player not part of game`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        // and: create an userA and log in
        val usernameA = createUser(client)
        val tokenA = login(client, usernameA)

        // and: create an userB and log in
        val usernameB = createUser(client)
        val tokenB = login(client, usernameB)

        // and: userA and userB start game
        val gameId = matchmaking(client, tokenA, tokenB)

        // and: create an userC and log in
        val usernameC = createUser(client)
        val tokenC = login(client, usernameC)

        // when: a player who does not belong to the game tries to play
        // then: the response is 403
        val problem = client.put().uri("/api/games/$gameId/play/row/1/column/1")
            .header("Authorization", "Bearer $tokenC")
            .exchange()
            .expectStatus().isForbidden
            .expectHeader().contentType(MEDIA_TYPE_ERROR)
            .expectBody(Problem::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(Problem.Type.PLAYER_NOT_PART_OF_GAME, problem.type)
    }

    @Test
    fun `player giving up during game`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        // and: create an userA and log in
        val usernameA = createUser(client)
        val tokenA = login(client, usernameA)

        // and: create an userB and log in
        val usernameB = createUser(client)
        val tokenB = login(client, usernameB)

        // and: userA and userB start game
        val gameId = matchmaking(client, tokenA, tokenB)

        // when: userA gives up
        // then: the response is 200
        client.put().uri("/api/games/$gameId/giveup")
            .header("Authorization", "Bearer $tokenA")
            .exchange()
            .expectStatus().isOk

        // when: userB observe the state
        // then: the response is 200 with the victory of userB
        client.get().uri("/api/games/$gameId")
            .header("Authorization", "Bearer $tokenB")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MEDIA_TYPE_SIREN)
            .expectBody()
            .jsonPath("$.properties.gameState")
            .isEqualTo("WINNER_W")
    }

    @Test
    fun `player not part of game trying to give up`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        // and: create an userA and log in
        val usernameA = createUser(client)
        val tokenA = login(client, usernameA)

        // and: create an userB and log in
        val usernameB = createUser(client)
        val tokenB = login(client, usernameB)

        // and: userA and userB start game
        val gameId = matchmaking(client, tokenA, tokenB)

        // and: create an userC and log in
        val usernameC = createUser(client)
        val tokenC = login(client, usernameC)

        // when: userC tries to leave a game he doesn't belong to
        // then: the response is 403
        val problem = client.put().uri("/api/games/$gameId/giveup")
            .header("Authorization", "Bearer $tokenC")
            .exchange()
            .expectStatus().isForbidden
            .expectHeader().contentType(MEDIA_TYPE_ERROR)
            .expectBody(Problem::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(Problem.Type.PLAYER_NOT_PART_OF_GAME, problem.type)
    }

    @Test
    fun `giving up on game that does not exist`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        // and: create an userA and log in
        val usernameA = createUser(client)
        val tokenA = login(client, usernameA)

        // and: a random game id
        val gameId = UUID.randomUUID()

        // when: userA tries to leave a game that doesn't exist
        // then: the response is 404
        val problem = client.put().uri("/api/games/$gameId/giveup")
            .header("Authorization", "Bearer $tokenA")
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().contentType(MEDIA_TYPE_ERROR)
            .expectBody(Problem::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(Problem.Type.GAME_NOT_FOUND, problem.type)
    }

    companion object {
        private const val MEDIA_TYPE_SIREN = "application/vnd.siren+json"
        private const val MEDIA_TYPE_ERROR = "application/problem+json"

        private const val PASSWORD = "Random123"

        private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

        private fun stringifyVariant(variant: VariantInfo): String {
            return objectMapper.writeValueAsString(variant)
        }
    }
}
