package pt.isel.gomoku.repository

/**
 * Represents a database transaction.
 */
interface Transaction {

    val usersRepository: UsersRepository

    val systemRepository: SystemRepository

    val gamesRepository: GamesRepository

    fun rollback()
}
