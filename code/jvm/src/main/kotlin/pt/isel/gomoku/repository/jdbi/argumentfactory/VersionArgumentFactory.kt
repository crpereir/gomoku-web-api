package pt.isel.gomoku.repository.jdbi.argumentfactory

import org.jdbi.v3.core.argument.AbstractArgumentFactory
import org.jdbi.v3.core.argument.Argument
import org.jdbi.v3.core.config.ConfigRegistry
import pt.isel.gomoku.domain.system.Version
import java.sql.Types

class VersionArgumentFactory : AbstractArgumentFactory<Version>(Types.VARCHAR) {

    override fun build(value: Version?, config: ConfigRegistry?): Argument {
        return Argument { position, statement, _ ->
            statement.setString(position, value.toString())
        }
    }
}
