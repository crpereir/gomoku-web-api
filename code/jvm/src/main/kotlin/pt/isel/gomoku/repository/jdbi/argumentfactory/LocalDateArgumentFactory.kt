package pt.isel.gomoku.repository.jdbi.argumentfactory

import kotlinx.datetime.LocalDate
import org.jdbi.v3.core.argument.AbstractArgumentFactory
import org.jdbi.v3.core.argument.Argument
import org.jdbi.v3.core.config.ConfigRegistry
import java.sql.Date
import java.sql.Types

class LocalDateArgumentFactory : AbstractArgumentFactory<LocalDate>(Types.DATE) {

    override fun build(value: LocalDate?, config: ConfigRegistry?): Argument {
        return Argument { position, statement, _ ->
            statement.setDate(position, Date.valueOf(value.toString()))
        }
    }
}
