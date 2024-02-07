package pt.isel.gomoku.repository.jdbi.mappers

import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.statement.StatementContext
import pt.isel.gomoku.domain.system.Version
import java.sql.ResultSet
import java.sql.SQLException

class VersionMapper : ColumnMapper<Version> {
    @Throws(SQLException::class)
    override fun map(rs: ResultSet, columnNumber: Int, ctx: StatementContext): Version {
        val version = rs.getString(columnNumber)
        val versionParts = version.split(".").map { it.toInt() }
        return Version(versionParts[0], versionParts[1], versionParts[2])
    }
}
