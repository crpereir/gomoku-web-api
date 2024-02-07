package pt.isel.gomoku.repository

/**
 * Allows for multiple database operations in only one transaction (atomic).
 */
interface TransactionManager {
    fun <R> run(block: (Transaction) -> R): R
}
