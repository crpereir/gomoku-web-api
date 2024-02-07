package pt.isel.gomoku.repository

import pt.isel.gomoku.domain.system.SystemInfo

/**
 * Represents database access related to system operations.
 */
interface SystemRepository {

    /**
     * Obtain the information and authors of the current version.
     */
    fun getSystemDetails(): SystemInfo
}
