package pt.isel.gomoku.domain.token

import java.security.MessageDigest
import java.util.Base64

/**
 * Encoder to convert tokens in hash using [MessageDigest], using SHA256 encoder.
 */
class Sha256TokenEncoder : TokenEncoder {

    override fun createValidationInformation(token: String): TokenValidationInfo =
        TokenValidationInfo(hash(token))

    private fun hash(input: String): String {
        val messageDigest = MessageDigest.getInstance("SHA256")
        return Base64.getUrlEncoder().encodeToString(
            messageDigest.digest(
                Charsets.UTF_8.encode(input).array()
            )
        )
    }
}
