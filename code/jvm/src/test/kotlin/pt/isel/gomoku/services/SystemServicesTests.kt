package pt.isel.gomoku.services

import org.jdbi.v3.core.Jdbi
import org.postgresql.ds.PGSimpleDataSource
import pt.isel.gomoku.repository.jdbi.JdbiTransactionManager
import pt.isel.gomoku.repository.jdbi.configureWithAppRequirements
import kotlin.test.Test
import kotlin.test.assertTrue

class SystemServicesTests {

    @Test
    fun `can retrieve system info`() {
        // given: a system service
        val systemService = createSystemService()

        // when: requesting system info
        val systemDetails = systemService.getSystemDetails()

        // then: retrieves correct system info
        assertTrue { systemDetails.version.toString() == "0.1.0" }
        assertTrue { systemDetails.authors != null }
        assertTrue { systemDetails.authors!!.isNotEmpty() }
    }

    companion object {

        private fun createSystemService() = SystemService(JdbiTransactionManager(jdbi))

        private val jdbi = Jdbi.create(
            PGSimpleDataSource().apply {
                setURL("jdbc:postgresql://localhost:5432/db?user=dbuser&password=changeit")
            }
        ).configureWithAppRequirements()
    }
}
