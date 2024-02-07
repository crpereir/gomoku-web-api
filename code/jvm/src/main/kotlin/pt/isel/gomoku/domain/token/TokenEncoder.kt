package pt.isel.gomoku.domain.token

/**
 * Contract for classes to convert tokens to information to validate it.
 */
interface TokenEncoder {
    fun createValidationInformation(token: String): TokenValidationInfo
}
