package pt.isel.gomoku.repository.jdbi

import org.jdbi.v3.core.Handle
import pt.isel.gomoku.repository.GamesRepository
import pt.isel.gomoku.repository.SystemRepository
import pt.isel.gomoku.repository.Transaction
import pt.isel.gomoku.repository.UsersRepository

class JdbiTransaction(
    private val handle: Handle
) : Transaction {

    override val usersRepository: UsersRepository = JdbiUsersRepository(handle)

    override val systemRepository: SystemRepository = JdbiSystemRepository(handle)

    override val gamesRepository: GamesRepository = JdbiGamesRepository(handle)

    override fun rollback() {
        handle.rollback()
    }
}
