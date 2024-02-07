package pt.isel.gomoku.http.pipeline

import jakarta.servlet.http.Cookie
import org.springframework.stereotype.Component
import pt.isel.gomoku.domain.users.AuthenticatedUser
import pt.isel.gomoku.services.UsersService

/**
 * Used to retrieve and validate authorization token through the request of the user with token.
 * If there is a user with given token, it is converted to AuthenticatedUser to give to
 * [AuthenticatedUserArgumentResolver].
 */
@Component
class RequestTokenProcessor(
    val usersService: UsersService
) {

    fun processAuthorizationHeaderValue(authorizationValue: String?): AuthenticatedUser? {
        if (authorizationValue == null) {
            return null
        }
        val parts = authorizationValue.trim().split(" ")
        if (parts.size != 2) {
            return null
        }
        if (parts[0].lowercase() != SCHEME) {
            return null
        }
        return usersService.getUserByToken(parts[1])?.let {
            AuthenticatedUser(
                it,
                parts[1]
            )
        }
    }

    fun processCookieValue(cookies: Array<Cookie>): AuthenticatedUser? {
        val cookie = cookies.firstOrNull { cookie -> cookie.name == TOKEN } ?: return null

        val cookieValue = cookie.value ?: return null

        return usersService.getUserByToken(cookieValue)?.let {
            AuthenticatedUser(
                it,
                cookieValue
            )
        }
    }

    companion object {
        const val TOKEN = "token"
        const val SCHEME = "bearer"
    }
}
