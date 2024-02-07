package pt.isel.gomoku.http.model.output

data class UserHomeOutputModel(
    val userId: Int,
    val username: String,
    val email: String
)
