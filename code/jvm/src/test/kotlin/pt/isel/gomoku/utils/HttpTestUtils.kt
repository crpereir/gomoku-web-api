package pt.isel.gomoku.utils

import org.springframework.test.web.reactive.server.WebTestClient
import pt.isel.gomoku.http.infra.SirenModel
import java.net.URI
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.assertTrue

private fun newTestUserName() = "user-${abs(Random.nextLong())}"
private fun newTestEmail(username: String) = "$username@gmail.com"

private const val PASSWORD = "Random123"

private const val MEDIA_TYPE_SIREN = "application/vnd.siren+json"

/**
 * Utility function that creates a user.
 * @return username of user.
 */
fun createUser(client: WebTestClient, password: String = PASSWORD): String {
    val username = newTestUserName()
    val email = newTestEmail(username)

    client.post().uri("/api/users")
        .bodyValue(mapOf("username" to username, "email" to email, "password" to password))
        .exchange()
        .expectStatus().isCreated
        .expectHeader().contentType(MEDIA_TYPE_SIREN)
        .expectHeader().value("location") { assertTrue(it.startsWith("/api/users/")) }

    return username
}

/**
 * Utility function that creates a token.
 * @return token of user.
 */
fun login(client: WebTestClient, username: String, password: String = PASSWORD): String {
    val properties = client.post().uri("api/users/token")
        .bodyValue(mapOf("identity" to username, "password" to password))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MEDIA_TYPE_SIREN)
        .expectBody(SirenModel::class.java)
        .returnResult()
        .responseBody!!
        .properties
    return (properties as LinkedHashMap<*, *>)["token"].toString()
}

/**
 * Utility function that simulates the first user who expresses desire in starting a game
 *  and who is left waiting in the waiting lobby.
 * @return link to status monitor.
 */
fun waitingForGame(client: WebTestClient, token: String): URI {
    val links = client.post().uri("/api/games")
        .header("Authorization", "Bearer $token")
        .exchange()
        .expectStatus().isAccepted
        .expectHeader().contentType(MEDIA_TYPE_SIREN)
        .expectBody(SirenModel::class.java)
        .returnResult()
        .responseBody!!
        .links

    return URI(links.first { link -> link.rel.any { it == "self" } }.href)
}

/**
 * Utility function that simulates the second user who expresses desire in starting a game
 *  and created the game.
 * @return id of created game.
 */
fun startGame(client: WebTestClient, token: String): String {
    val links = client.post().uri("/api/games")
        .header("Authorization", "Bearer $token")
        .exchange()
        .expectStatus().isCreated
        .expectHeader().contentType(MEDIA_TYPE_SIREN)
        .expectBody(SirenModel::class.java)
        .returnResult()
        .responseBody!!
        .links

    return links.first { link -> link.rel.any { it == "self" } }.href.substringAfter("/api/games/")
}

/**
 * Utility function that simulates matchmaking between two users using [waitingForGame] and [startGame] functions.
 * @return id of created game.
 */
fun matchmaking(client: WebTestClient, tokenA: String, tokenB: String): String {
    waitingForGame(client, tokenA)
    return startGame(client, tokenB)
}
