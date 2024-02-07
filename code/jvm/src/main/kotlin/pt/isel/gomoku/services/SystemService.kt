package pt.isel.gomoku.services

import org.springframework.stereotype.Component
import pt.isel.gomoku.domain.system.SystemInfo
import pt.isel.gomoku.repository.TransactionManager

/**
 * Represents different operations related to system.
 */
@Component
class SystemService(
    private val transactionManager: TransactionManager
) {

    /**
     * Obtaining the system information of current version.
     */
    fun getSystemDetails(): SystemInfo {
        return transactionManager.run {
            val systemRepository = it.systemRepository
            val systemDetails = systemRepository.getSystemDetails()
            checkNotNull(systemDetails.authors)
            systemDetails
        }
    }
}
