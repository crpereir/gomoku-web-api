package pt.isel.gomoku.domain.users

sealed class UserCreationError {
    object UserAlreadyExists : UserCreationError()
    object EmailNotValid : UserCreationError()
    object InsecurePassword : UserCreationError()
}

object UserNotFound
