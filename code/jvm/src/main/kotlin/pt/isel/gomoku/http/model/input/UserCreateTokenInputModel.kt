package pt.isel.gomoku.http.model.input

data class UserCreateTokenInputModel(
    val identity: String,
    val password: String
)
