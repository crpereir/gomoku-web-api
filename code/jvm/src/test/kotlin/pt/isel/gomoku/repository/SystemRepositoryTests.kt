package pt.isel.gomoku.repository

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import pt.isel.gomoku.repository.jdbi.JdbiSystemRepository
import pt.isel.gomoku.repository.jdbi.configureWithAppRequirements
import kotlin.test.assertTrue

class SystemRepositoryTests {

    @Test
    fun `can retrieve system info`() = runWithHandle { handle ->
        // given: a SystemRepository
        val repo = JdbiSystemRepository(handle)

        // when: requesting system info
        val systemDetails = repo.getSystemDetails()

        // then: retrieves correct system info
        assertTrue { systemDetails.version.toString() == "0.1.0" }
        assertTrue { systemDetails.authors != null }
        assertTrue { systemDetails.authors!!.isNotEmpty() }
    }

    companion object {
        private val jdbi = Jdbi.create(
            PGSimpleDataSource().apply {
                setURL("jdbc:postgresql://localhost:5432/db?user=dbuser&password=changeit")
            }
        ).configureWithAppRequirements()

        private fun runWithHandle(block: (Handle) -> Unit) = jdbi.useTransaction<Exception>(block)
    }
}
