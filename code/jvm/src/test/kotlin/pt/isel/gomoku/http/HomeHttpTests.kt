package pt.isel.gomoku.http

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HomeHttpTests {

    @LocalServerPort
    var port: Int = 0

    @Test
    fun `can retrieve system details`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // when: obtaining home resource
        // then: the response is a 200
        client.get().uri("/")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MEDIA_TYPE_SIREN)
    }

    companion object {
        private const val MEDIA_TYPE_SIREN = "application/vnd.siren+json"
    }
}
