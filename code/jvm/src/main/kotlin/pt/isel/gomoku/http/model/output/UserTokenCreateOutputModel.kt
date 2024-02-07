package pt.isel.gomoku.http.model.output

data class UserTokenCreateOutputModel(
    val token: String,
    val expiration: String
)
