package pt.isel.gomoku.http

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient
import pt.isel.gomoku.http.infra.SirenModel
import pt.isel.gomoku.utils.createUser
import pt.isel.gomoku.utils.login
import pt.isel.gomoku.utils.startGame
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserJourneyTests {

    // One of the very few places where we use property injection
    @LocalServerPort
    var port: Int = 0

    private val numberOfUsers = 10

    // NOTE: code delimited by '//---' are actions by another user to simulate interaction.
    @Test
    fun `simulates _numberOfUsers_ user journey using siren links and siren actions`() {
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        repeat(numberOfUsers) {
            // GET HOME
            val sirenHome = unauthenticatedRoute(
                client = client,
                path = "/api/",
                method = Method.GET
            )

            // GET ABOUT
            unauthenticatedRoute(
                client = client,
                path = sirenHome.links.first { l -> l.rel.contains(BASE_LINK_RELATION + "about") }.href,
                method = Method.GET
            )

            // GET RANKING
            unauthenticatedRoute(
                client = client,
                path = sirenHome.links.first { l -> l.rel.contains(BASE_LINK_RELATION + "ranking") }.href,
                method = Method.GET
            )

            // POST SIGNUP
            val username = newTestUserName()
            val email = newTestEmail(username)
            val password = "Random123"
            val sirenSignup = unauthenticatedRoute(
                client = client,
                path = sirenHome.actions.first { a -> a.name == "signup" }.href,
                method = Method.POST,
                body = mapOf("username" to username, "email" to email, "password" to password)
            )

            // GET USER
            unauthenticatedRoute(
                client = client,
                method = Method.GET,
                path = sirenSignup.links.first { l -> l.rel.contains("self") }.href
            )

            // GET STATISTICS
            val userId = sirenSignup.links.first { l -> l.rel.contains("self") }.href.substringAfter("users/")
            unauthenticatedRoute(
                client = client,
                method = Method.GET,
                path = "/api/users/$userId/statistics"
            )

            // POST LOGIN
            val token = login(client, username, password)

            // GET USER HOME
            val sirenUserHome = authenticatedRoute(
                client = client,
                method = Method.GET,
                path = "api/me",
                token = token
            )

            // GET VARIANTS
            val sirenVariants = authenticatedRoute(
                client = client,
                method = Method.GET,
                path = sirenUserHome.links.first { l -> l.rel.contains(BASE_LINK_RELATION + "variants") }.href,
                token = token
            )

            // POST START GAME
            val sirenMonitor = authenticatedRoute(
                client = client,
                method = Method.POST,
                path = sirenVariants.actions.first { a -> a.name == "start-game" }.href,
                token = token
            )

            // GET MONITOR
            val sirenMonitorAgain = authenticatedRoute(
                client = client,
                method = Method.GET,
                path = sirenMonitor.links.first { l -> l.rel.contains("self") }.href,
                token = token
            )

            // ---------------------------------------------------------------------------------------------------------
            val anotherUsername = createUser(client)
            val anotherToken = login(client, anotherUsername)
            startGame(client, anotherToken)
            // ---------------------------------------------------------------------------------------------------------

            // GET MONITOR
            val sirenMonitorAgain2 = authenticatedRoute(
                client = client,
                method = Method.GET,
                path = sirenMonitorAgain.links.first { l -> l.rel.contains("self") }.href,
                token = token
            )

            // GET GAME [1]
            val sirenGame = authenticatedRoute(
                client = client,
                method = Method.GET,
                path = sirenMonitorAgain2.links.first { l -> l.rel.contains("self") }.href,
                token = token
            )

            val gameId = sirenGame.links.first { l -> l.rel.contains("self") }.href.substringAfter("/games/")
            val hrefPlay = sirenGame.actions.first { a -> a.name == "play" }.href.replace("{id}", gameId)

            // PUT PLAY'S
            repeat(5) {
                authenticatedRoute(
                    client = client,
                    method = Method.PUT,
                    path = hrefPlay.replace("{row}", 0.toString()).replace("{column}", it.toString()),
                    token = token
                )

                // -----------------------------------------------------------------------------------------------------
                if (it != 4) {
                    authenticatedRoute(
                        client = client,
                        method = Method.PUT,
                        path = hrefPlay.replace("{row}", 1.toString()).replace("{column}", it.toString()),
                        token = anotherToken
                    )
                }
                // -----------------------------------------------------------------------------------------------------
            }

            // GET GAME [2]
            val sirenUpdatedGame = authenticatedRoute(
                client = client,
                method = Method.GET,
                path = sirenMonitorAgain2.links.first { l -> l.rel.contains("self") }.href,
                token = token
            )
            assertEquals("WINNER_B", (sirenUpdatedGame.properties as LinkedHashMap<*, *>)["gameState"].toString())

            // DELETE LOGOUT
            client
                .post()
                .uri("api/logout")
                .header("Authorization", "Bearer $token")
                .exchange()
                .expectStatus().isOk
        }
    }

    companion object {
        private const val BASE_LINK_RELATION =
            "https://github.com/isel-leic-daw/2023-daw-leic51d-07/tree/main/docs/api/rel/"

        private fun newTestUserName() = "user-${abs(Random.nextLong())}"
        private fun newTestEmail(username: String) = "$username@gmail.com"
    }

    enum class Method {
        GET, POST, PUT
    }

    private fun unauthenticatedRoute(
        client: WebTestClient,
        path: String,
        method: Method,
        body: Map<String, String>? = null
    ): SirenModel<*> {
        val request = when (method) {
            Method.GET -> client.get().uri(path).exchange()
            Method.POST -> client.post().uri(path).bodyValue(body!!).exchange()
            Method.PUT -> client.put().uri(path).exchange()
        }

        return request
            .expectStatus().is2xxSuccessful
            .expectBody(SirenModel::class.java)
            .returnResult()
            .responseBody!!
    }

    private fun authenticatedRoute(
        client: WebTestClient,
        path: String,
        method: Method,
        token: String
    ): SirenModel<*> {
        val request = when (method) {
            Method.GET -> client.get()
            Method.POST -> client.post()
            Method.PUT -> client.put()
        }

        return request
            .uri(path)
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody(SirenModel::class.java)
            .returnResult()
            .responseBody!!
    }
}
