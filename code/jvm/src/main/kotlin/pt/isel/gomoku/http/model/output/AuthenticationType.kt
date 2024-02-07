package pt.isel.gomoku.http.model.output

enum class AuthenticationType {

    NONE, BEARER, COOKIE;

    companion object {
        fun needAuthentication() = listOf(BEARER, COOKIE)
    }
}
