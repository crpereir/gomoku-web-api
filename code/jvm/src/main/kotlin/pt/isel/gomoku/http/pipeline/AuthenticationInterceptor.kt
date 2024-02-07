package pt.isel.gomoku.http.pipeline

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import pt.isel.gomoku.domain.users.AuthenticatedUser

/**
 * Verifies if the request has an authorization tag before entering the handler.
 * If it doesn't, it is "short-circuited" with 401 status code.
 * If it does, [RequestTokenProcessor] will obtain the user of token.
 */
@Component
class AuthenticationInterceptor(
    private val tokenProcessor: RequestTokenProcessor
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (handler is HandlerMethod && handler.methodParameters.any {
            it.parameterType == AuthenticatedUser::class.java
        }
        ) {
            // decide where token is
            val cookies = request.cookies
            val user = if (cookies != null) {
                // validate cookie and enforce authentication
                tokenProcessor
                    .processCookieValue(cookies)
            } else {
                // validate authorization header and enforce authentication
                tokenProcessor
                    .processAuthorizationHeaderValue(request.getHeader(NAME_AUTHORIZATION_HEADER))
            }
            return if (user == null) {
                response.status = 401

                response.addHeader(NAME_WWW_AUTHENTICATE_HEADER, RequestTokenProcessor.SCHEME)
                false
            } else {
                AuthenticatedUserArgumentResolver.addUserTo(user, request)
                true
            }
        }

        return true
    }

    companion object {

        private val logger = LoggerFactory.getLogger(AuthenticationInterceptor::class.java)
        const val NAME_AUTHORIZATION_HEADER = "Authorization"
        private const val NAME_WWW_AUTHENTICATE_HEADER = "WWW-Authenticate"
    }
}
