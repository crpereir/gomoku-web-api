package pt.isel.gomoku.http

import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SystemHttpTests {

    @LocalServerPort
    var port: Int = 0

    @Test
    fun `can retrieve system details`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // when: retrieving system details
        // then: the response is a 200 with proper body
        client.get().uri("/system")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MEDIA_TYPE_SIREN)
            .expectBody()
            .jsonPath("$.properties.version")
            .isEqualTo("0.1.0")
            .jsonPath("$.properties.createdAt")
            .isEqualTo("2023-10-08")
            .jsonPath("$.properties.authors[*].firstName")
            .value(
                Matchers.everyItem(
                    Matchers.oneOf("João", "André", "Carolina")
                )
            )
            .jsonPath("$.properties.authors[*].surname")
            .value(
                Matchers.everyItem(
                    Matchers.oneOf("Bonacho", "Gonçalves", "Pereira")
                )
            )
            .jsonPath("$.properties.authors[*].email")
            .value(
                Matchers.everyItem(
                    Matchers.oneOf("a49437@alunos.isel.pt", "a49464@alunos.isel.pt", "a49470@alunos.isel.pt")
                )
            )
    }

    companion object {
        private const val MEDIA_TYPE_SIREN = "application/vnd.siren+json"
    }
}
