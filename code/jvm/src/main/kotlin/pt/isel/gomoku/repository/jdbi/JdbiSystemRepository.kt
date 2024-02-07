package pt.isel.gomoku.repository.jdbi

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.slf4j.LoggerFactory
import pt.isel.gomoku.domain.system.Author
import pt.isel.gomoku.domain.system.SystemInfo
import pt.isel.gomoku.repository.SystemRepository

/**
 * Represents database access related to system operations using JDBI.
 */
class JdbiSystemRepository(
    private val handle: Handle
) : SystemRepository {

    override fun getSystemDetails(): SystemInfo {
        val infoWithoutAuthors = handle.select(
            """
            select version, created_at from dbo.SystemInfo order by created_at desc limit 1;
            """
        )
            .mapTo<SystemInfo>()
            .one()
        return infoWithoutAuthors.copy(
            authors = handle.select(
                """
                    select first_name, surname, email
                    from dbo.Authors a join dbo.CreatedVersion cv on (a.id = cv.author_id)
                    where cv.system_ver like :version
                    """
            )
                .bind("version", infoWithoutAuthors.version)
                .mapTo<Author>()
                .list()
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JdbiSystemRepository::class.java)
    }
}
