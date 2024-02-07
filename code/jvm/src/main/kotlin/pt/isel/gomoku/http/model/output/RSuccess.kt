package pt.isel.gomoku.http.model.output

import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import org.springframework.http.ResponseEntity
import pt.isel.gomoku.http.infra.SirenModel
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder

class RSuccess {

    companion object {
        private const val MEDIA_TYPE = "application/vnd.siren+json"

        fun <T> response(
            status: Int = 200,
            headers: Map<String, String>? = null,
            body: SirenModel<T>,
            cookies: List<String>? = null
        ): ResponseEntity<*> {
            val responseEntity = ResponseEntity
                .status(status)
                .header("Content-Type", MEDIA_TYPE)

            headers?.let {
                headers.forEach { header ->
                    responseEntity.header(header.key, header.value)
                }
            }

            cookies?.let {
                cookies.forEach { cookie ->
                    responseEntity.header("Set-Cookie", cookie)
                }
            }

            return responseEntity.body(body)
        }
    }
}

fun generateSessionCookies(userId: Int, username: String, token: String, expirationDate: Instant) = listOf(
    formatCookie(
        name = "token",
        value = token,
        expires = expirationDate,
        sameSite = "Strict",
        path = "/",
        secure = true,
        httpOnly = true
    ),
    formatCookie(
        name = "login",
        value = "$userId-$username",
        expires = expirationDate,
        sameSite = "None",
        path = "/",
        secure = true,
        httpOnly = false
    )
)

fun formatCookie(
    name: String,
    value: String,
    expires: Instant,
    sameSite: String,
    path: String,
    secure: Boolean,
    httpOnly: Boolean
): String {
    val formatter = DateTimeFormatterBuilder()
        .appendPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
        .toFormatter()
        .withZone(ZoneId.of("GMT"))

    val cookieExpiresFormat = formatter.format(expires.toJavaInstant())

    val cookie = StringBuilder("$name=$value; Expires=$cookieExpiresFormat; SameSite=$sameSite; Path=$path")
    if (secure) cookie.append("; Secure")
    if (httpOnly) cookie.append("; HttpOnly")

    return cookie.toString()
}
