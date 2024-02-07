package pt.isel.gomoku.repository.jdbi.mappers

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toKotlinLocalDate
import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.sql.SQLException

class LocalDateMapper : ColumnMapper<LocalDate> {
    @Throws(SQLException::class)
    override fun map(rs: ResultSet, columnNumber: Int, ctx: StatementContext): LocalDate =
        rs.getDate(columnNumber).toLocalDate().toKotlinLocalDate()
}
