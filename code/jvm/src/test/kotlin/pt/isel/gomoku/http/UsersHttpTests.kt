package pt.isel.gomoku.http

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient
import pt.isel.gomoku.http.infra.SirenModel
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UsersHttpTests {

    // One of the very few places where we use property injection
    @LocalServerPort
    var port: Int = 0

    @Test
    fun `can create a user, get the user and get statistics`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // and: a random user
        val username = newTestUserName()
        val email = newTestEmail(username)
        val password = "Random123"

        // variable to affect with id after the user is created
        val id: Int

        // when: creating an user
        // then: the response is a 201 with a proper Location header and body
        val link = client.post().uri("/users")
            .bodyValue(mapOf("username" to username, "email" to email, "password" to password))
            .exchange()
            .expectStatus().isCreated
            .expectHeader().contentType(MEDIA_TYPE_SIREN)
            .expectHeader().value("location") { assertTrue(it.startsWith("/api/users/")) }
            .expectBody(SirenModel::class.java)
            .returnResult()
            .responseBody!!
            .links.first { link -> link.rel.any { it == "self" } }.href

        assertTrue { link.startsWith("/api/users/") }
        id = link.substringAfter("/api/users/").toInt()

        // when: get by id
        // then: the response is a 200 with a proper Body
        client.get().uri("/users/$id")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MEDIA_TYPE_SIREN)
            .expectBody()
            .jsonPath("$.properties.username")
            .isEqualTo(username)
            .jsonPath("$.properties.email")
            .isEqualTo(email)

        // when: get the statistics
        // then: the response is a 200
        client.get().uri("/users/$id/statistics")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MEDIA_TYPE_SIREN)
    }

    @Test
    fun `can't create two users with the same username, email or both`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // and: a random user
        val username = newTestUserName()
        val email = newTestEmail(username)
        val password = "Random123"

        // when: creating an user
        val link = client.post().uri("/users")
            .bodyValue(mapOf("username" to username, "email" to email, "password" to password))
            .exchange()
            .expectStatus().isCreated
            .expectHeader().contentType(MEDIA_TYPE_SIREN)
            .expectBody(SirenModel::class.java)
            .returnResult()
            .responseBody!!
            .links.first { link -> link.rel.any { it == "self" } }.href

        assertTrue { link.startsWith("/api/users/") }

        // when: creating a user with the same username
        // then: the response is a 400 with a proper Header Content-Type
        client.post().uri("/users")
            .bodyValue(mapOf("username" to username, "email" to "different$email", "password" to password))
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MEDIA_TYPE_ERROR)

        // when: creating a user with the same email
        // then: the response is a 400 with a proper Header Content-Type
        client.post().uri("/users")
            .bodyValue(mapOf("username" to "different$username", "email" to email, "password" to password))
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MEDIA_TYPE_ERROR)

        // when: creating a user with the same username and the same email
        // then: the response is a 400 with a proper Header Content-Type
        client.post().uri("/users")
            .bodyValue(mapOf("username" to username, "email" to email, "password" to password))
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MEDIA_TYPE_ERROR)
    }

    @Test
    fun `can't create a user with an insecure password`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // and: a random user
        val username = newTestUserName()
        val email = newTestEmail(username)
        val password = "random"

        // when: creating a user with an insecure password
        // then: the response is a 400 with a proper Header Content-Type
        client.post().uri("/users")
            .bodyValue(mapOf("username" to username, "email" to email, "password" to password))
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MEDIA_TYPE_ERROR)
    }

    @Test
    fun `can't create a user with an invalid email format`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // and: a random user
        val username = newTestUserName()
        val email = "thisIsNotAnEmail"
        val password = "Random123"

        // when: creating a user with an invalid email format
        // then: the response is a 400 with a proper Header Content-Type
        client.post().uri("/users")
            .bodyValue(mapOf("username" to username, "email" to email, "password" to password))
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MEDIA_TYPE_ERROR)
    }

    @Test
    fun `can't get by id or get statistics for a user that does not exist`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // and: a user id not in system
        val id = 10000

        // when: get a user by id that not exist
        // then: the response is a 404 with a proper Header Content-Type
        client.get().uri("/users/$id")
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().contentType(MEDIA_TYPE_ERROR)

        // when: get user statistics that not exist
        // then: the response is a 404 with a proper Header Content-Type
        client.get().uri("/users/$id/statistics")
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().contentType(MEDIA_TYPE_ERROR)
    }

    @Test
    fun `can get users ranking`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // when: get users ranking
        // then: the response is a 200
        client.get().uri("/users/ranking")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MEDIA_TYPE_SIREN)
    }

    @Test
    fun `can create an user, obtain a token (with username), and access user home, and logout`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // and: a random user
        val username = newTestUserName()
        val email = newTestEmail(username)
        val password = "Random123"

        // when: creating an user
        // then: the response is a 201 with a proper Location header
        val link = client.post().uri("/users")
            .bodyValue(mapOf("username" to username, "email" to email, "password" to password))
            .exchange()
            .expectStatus().isCreated
            .expectHeader().contentType(MEDIA_TYPE_SIREN)
            .expectHeader().value("location") { assertTrue(it.startsWith("/api/users/")) }
            .expectBody(SirenModel::class.java)
            .returnResult()
            .responseBody!!
            .links.first { link -> link.rel.any { it == "self" } }.href

        assertTrue { link.startsWith("/api/users/") }

        // when: creating a token
        // then: the response is a 200
        val result = client.post().uri("/users/token")
            .bodyValue(
                mapOf(
                    "identity" to username,
                    "password" to password
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MEDIA_TYPE_SIREN)
            .expectBody(SirenModel::class.java)
            .returnResult()
            .responseBody!!
            .properties

        val token = (result as LinkedHashMap<*, *>)["token"].toString()

        // when: getting the user home with a valid token
        // then: the response is a 200 with the proper representation
        client.get().uri("/me")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MEDIA_TYPE_SIREN)
            .expectBody()
            .jsonPath("properties.username").isEqualTo(username)
            .jsonPath("properties.email").isEqualTo(email)

        // when: getting the user home with a valid token in cookie
        // then: the response is a 200 with the proper representation
        client.get().uri("/me")
            .header("Cookie", "token=$token;")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MEDIA_TYPE_SIREN)
            .expectBody()
            .jsonPath("properties.username").isEqualTo(username)
            .jsonPath("properties.email").isEqualTo(email)

        // when: getting the user home with an invalid token
        // then: the response is a 401 with the proper problem
        client.get().uri("/me")
            .header("Authorization", "Bearer $token-invalid")
            .exchange()
            .expectStatus().isUnauthorized
            .expectHeader().valueEquals("WWW-Authenticate", "bearer")

        // when: logout
        // then: the response is a 200
        client.post().uri("/logout")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `can't create a token with invalid identity or password`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // and: a random user
        val username = newTestUserName()
        val email = newTestEmail(username)
        val password = "Random123"

        // when: creating an user
        // then: the response is a 201 with a proper Location header
        val link = client.post().uri("/users")
            .bodyValue(mapOf("username" to username, "email" to email, "password" to password))
            .exchange()
            .expectStatus().isCreated
            .expectHeader().contentType(MEDIA_TYPE_SIREN)
            .expectHeader().value("location") { assertTrue(it.startsWith("/api/users/")) }
            .expectBody(SirenModel::class.java)
            .returnResult()
            .responseBody!!
            .links.first { link -> link.rel.any { it == "self" } }.href

        assertTrue { link.startsWith("/api/users/") }

        // when: creating a token with wrong identity
        // then: the response is a 400
        client.post().uri("/users/token")
            .bodyValue(
                mapOf(
                    "identity" to "$username-wrong",
                    "password" to password
                )
            )
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MEDIA_TYPE_ERROR)

        // when: creating a token with wrong password
        // then: the response is a 400
        client.post().uri("/users/token")
            .bodyValue(
                mapOf(
                    "identity" to username,
                    "password" to "$password-wrong"
                )
            )
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MEDIA_TYPE_ERROR)
    }

    companion object {
        private const val MEDIA_TYPE_SIREN = "application/vnd.siren+json"
        private const val MEDIA_TYPE_ERROR = "application/problem+json"

        private fun newTestUserName() = "user-${abs(Random.nextLong())}"
        private fun newTestEmail(username: String) = "$username@gmail.com"
    }
}
