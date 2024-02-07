package pt.isel.gomoku.domain.users

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import pt.isel.gomoku.domain.token.Token
import pt.isel.gomoku.domain.token.TokenEncoder
import pt.isel.gomoku.domain.token.TokenValidationInfo
import java.security.SecureRandom
import java.util.*

/**
 * Functions that help validate User.
 */
@Component
class UsersDomain(
    private val passwordEncoder: PasswordEncoder,
    private val tokenEncoder: TokenEncoder,
    private val config: UsersDomainConfig
) {
    val limitRanking = config.limitUsersRanking

    val maxNumberOfTokensPerUser = config.maxTokensPerUser

    fun canBeEmail(email: String): Boolean =
        email.contains("@") && email.contains(".")

    fun isSafePassword(password: String): Boolean =
        password.length >= 6 &&
            password.any { it.isDigit() } &&
            password.any { it.isLowerCase() } &&
            password.any { it.isUpperCase() }

    fun createPasswordValidationInformation(password: String): PasswordValidationInfo =
        PasswordValidationInfo(passwordEncoder.encode(password))

    fun validatePassword(password: String, validationInfo: PasswordValidationInfo): Boolean =
        passwordEncoder.matches(password, validationInfo.validationInfo)

    fun generateTokenValue(): String =
        ByteArray(config.tokenSizeInBytes).let { byteArray ->
            SecureRandom.getInstanceStrong().nextBytes(byteArray)
            Base64.getUrlEncoder().encodeToString(byteArray)
        }

    fun canBeToken(token: String): Boolean = try {
        Base64.getUrlDecoder()
            .decode(token).size == config.tokenSizeInBytes
    } catch (ex: IllegalArgumentException) {
        false
    }

    fun isTokenTimeValid(clock: Clock, token: Token): Boolean {
        val now = clock.now()
        return token.createdAt <= now &&
            (now - token.createdAt) <= config.tokenTtl &&
            (now - token.lastUsedAt) <= config.tokenRollingTtl
    }

    fun getTokenExpiration(token: Token): Instant {
        val absoluteExpiration = token.createdAt + config.tokenTtl
        val rollingExpiration = token.lastUsedAt + config.tokenRollingTtl
        return if (absoluteExpiration < rollingExpiration) {
            absoluteExpiration
        } else {
            rollingExpiration
        }
    }

    fun createTokenValidationInformation(token: String): TokenValidationInfo =
        tokenEncoder.createValidationInformation(token)
}
