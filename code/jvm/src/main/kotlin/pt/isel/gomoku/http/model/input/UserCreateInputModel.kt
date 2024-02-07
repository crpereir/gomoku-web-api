package pt.isel.gomoku.http.model.input

data class UserCreateInputModel(
    val username: String,
    val email: String,
    val password: String
)
