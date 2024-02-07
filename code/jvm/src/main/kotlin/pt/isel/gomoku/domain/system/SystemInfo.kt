package pt.isel.gomoku.domain.system

import kotlinx.datetime.LocalDate

/**
 * Used to aggregate system information to its respective authors to then show when user requests system info.
 */
data class SystemInfo(
    val version: Version,
    val createdAt: LocalDate,
    val authors: List<Author>? = null
)
