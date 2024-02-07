package pt.isel.gomoku.domain.token

sealed class TokenCreationError {
    object InvalidCredentials : TokenCreationError()
}
