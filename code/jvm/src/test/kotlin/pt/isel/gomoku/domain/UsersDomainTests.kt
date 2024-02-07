package pt.isel.gomoku.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.gomoku.domain.token.Sha256TokenEncoder
import pt.isel.gomoku.domain.token.Token
import pt.isel.gomoku.domain.users.UsersDomain
import pt.isel.gomoku.domain.users.UsersDomainConfig
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

class UsersDomainTests {

    @Test
    fun `can be a valid email`() {
        // Arrange
        // Act
        // Assert
        assertTrue(usersDomain.canBeEmail("user1@isel.pt"))
        assertFalse(usersDomain.canBeEmail("user1isel.pt"))
        assertFalse(usersDomain.canBeEmail("user1@iselpt"))
    }

    @Test
    fun `verify if password is safe`() {
        // Arrange
        // Act
        // Assert
        assertTrue(usersDomain.isSafePassword("passUsername1"))
        assertFalse(usersDomain.isSafePassword("pass"))
        assertFalse(usersDomain.isSafePassword("PASSUSERNAME"))
        assertFalse(usersDomain.isSafePassword("passusername1"))
        assertFalse(usersDomain.isSafePassword("passUsername"))
    }

    @Test
    fun `create password validation info and validate password`() {
        // Arrange
        // Act
        val passwordValidationInfo = usersDomain.createPasswordValidationInformation("passUsername1")

        // Assert
        assertTrue(usersDomain.validatePassword("passUsername1", passwordValidationInfo))
        assertFalse(usersDomain.validatePassword("pass", passwordValidationInfo))
    }

    @Test
    fun `can create and validate tokens`() {
        // Arrange
        // Act
        val tokenValue = usersDomain.generateTokenValue()
        val tokenValidationInfo = usersDomain.createTokenValidationInformation(tokenValue)
        val tokenValidA = Token(tokenValidationInfo, 0, clock.now(), clock.now())
        val tokenValidB = Token(tokenValidationInfo, 0, Instant.DISTANT_PAST, clock.now())
        val tokenInvalid = Token(tokenValidationInfo, 0, clock.now(), Instant.DISTANT_PAST)

        // Assert
        assertTrue(usersDomain.canBeToken(tokenValue))
        assertFalse(usersDomain.canBeToken("thisIsNotAToken"))
        assertTrue(usersDomain.isTokenTimeValid(clock, tokenValidA))
        assertFalse(usersDomain.isTokenTimeValid(clock, tokenInvalid))

        assertEquals(tokenValidA.lastUsedAt + config.tokenRollingTtl, usersDomain.getTokenExpiration(tokenValidA))
        assertEquals(tokenValidB.createdAt + config.tokenTtl, usersDomain.getTokenExpiration(tokenValidB))
    }

    companion object {
        private val passwordEncoder = BCryptPasswordEncoder()
        private val tokenEncoder = Sha256TokenEncoder()
        private val clock = Clock.System
        private val config = UsersDomainConfig(
            tokenSizeInBytes = 256 / 8,
            tokenTtl = 24.hours,
            tokenRollingTtl = 1.hours,
            maxTokensPerUser = 3,
            limitUsersRanking = 10
        )
        private val usersDomain = UsersDomain(passwordEncoder, tokenEncoder, config)
    }
}
