package pt.isel.gomoku.concurrency

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import pt.isel.gomoku.utils.createUser
import pt.isel.gomoku.utils.login
import pt.isel.gomoku.utils.startGame
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.Test
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcurrencyTests {

    // One of the very few places where we use property injection
    @LocalServerPort
    var port: Int = 0

    @Test
    fun `can control concurrency in matchmaking algorithm`() {
        // error control
        val errors = ConcurrentLinkedQueue<Exception>()
        val failures = ConcurrentLinkedQueue<AssertionError>()

        val numberOfGames = 10
        val ths = ConcurrentLinkedQueue<Thread>()

        // ensure there are no server errors
        repeat(numberOfGames) {
            val th1 = Thread {
                try {
                    val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
                    val username = createUser(client)
                    val token = login(client, username)
                    startGame(client, token)
                } catch (e: AssertionError) {
                    failures.add(e)
                } catch (e: Exception) {
                    errors.add(e)
                }
            }
            th1.start()
            ths.add(th1)
        }
        ths.forEach { it.join() }

        // error checking
        if (!failures.isEmpty()) throw failures.peek()
        if (!errors.isEmpty()) throw errors.peek()

        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        // do maths with the users who were there.
        repeat(numberOfGames) {
            val username = createUser(client)
            val token = login(client, username)
            startGame(client, token)
        }
    }

    @Test
    fun `can control concurrency in user creation`() {
        // error control
        val errors = ConcurrentLinkedQueue<Exception>()
        val failures = ConcurrentLinkedQueue<AssertionError>()

        val numberOfUsers = 10
        val ths = ConcurrentLinkedQueue<Thread>()

        // ensure there are no server errors
        repeat(numberOfUsers) {
            val th1 = Thread {
                try {
                    val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
                    createUser(client, USERNAME, EMAIL, PASSWORD)
                } catch (e: AssertionError) {
                    println(e)
                    failures.add(e)
                } catch (e: Exception) {
                    errors.add(e)
                }
            }
            th1.start()
            ths.add(th1)
        }
        ths.forEach { it.join() }

        // error checking
        if (!failures.isEmpty()) throw errors.peek()
        if (!errors.isEmpty()) throw errors.peek()
    }

    companion object {

        private const val USERNAME = "allUsersWithThisUsername"
        private const val EMAIL = "allUsersWithThisEamil@isel.pt"
        private const val PASSWORD = "Random123"

        private fun startGame(client: WebTestClient, token: String) {
            client.post().uri("/api/games")
                .header("Authorization", "Bearer $token")
                .exchange()
                .expectStatus().is2xxSuccessful
        }

        private fun createUser(client: WebTestClient, username: String, email: String, password: String) {
            val response = client.post().uri("/api/users")
                .bodyValue(mapOf("username" to username, "email" to email, "password" to password))
                .exchange()
                .returnResult<String>()

            // User was created or already existed.
            assertTrue { response.status.is2xxSuccessful || response.status.is4xxClientError }
        }
    }
}
