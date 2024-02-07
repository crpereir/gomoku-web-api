package pt.isel.gomoku.domain.users

/**
 * Represents a user.
 * Note that password is not stored, only the information that validates inputted password.
 */
data class User(
    val id: Int,
    val username: String,
    val email: String,
    val passwordValidation: PasswordValidationInfo
)
