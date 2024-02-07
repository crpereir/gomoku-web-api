package pt.isel.gomoku.http.model.output

import pt.isel.gomoku.domain.system.Author

data class SystemInfoOutputModel(
    val version: String,
    val createdAt: String,
    val authors: List<Author>
)
