package pt.isel.gomoku.http

import org.springframework.web.util.UriTemplate
import java.net.URI
import java.util.*

/**
 * Represents all URIs of API.
 */
object Uris {

    const val PREFIX = "/api"

    object Home {
        const val HOME = "$PREFIX/"

        fun home() = URI(HOME)
    }

    object Users {
        const val CREATE = "$PREFIX/users"
        const val TOKEN = "$PREFIX/users/token"
        const val RANKING = "$PREFIX/users/ranking"
        const val GET_BY_ID = "$PREFIX/users/{id}"
        const val STATISTICS = "$PREFIX/users/{id}/statistics"
        const val LOGOUT = "$PREFIX/logout"
        const val USER_HOME = "$PREFIX/me"

        fun signup() = URI(CREATE)
        fun login() = URI(TOKEN)
        fun ranking() = URI(RANKING)
        fun ranking(page: Int) = URI("$RANKING?page=$page")
        fun byId(id: Int) = UriTemplate(GET_BY_ID).expand(id)
        fun statistics(id: Int) = UriTemplate(STATISTICS).expand(id)
        fun statisticsTemplate() = UriTemplate(STATISTICS)
        fun logout() = URI(LOGOUT)
        fun home() = URI(USER_HOME)
    }

    object System {
        const val ABOUT = "$PREFIX/system"

        fun about() = URI(ABOUT)
    }

    object Games {
        const val VARIANTS = "$PREFIX/games/variants"
        const val VARIANT = "$PREFIX/games/variants/{id}"
        const val START = "$PREFIX/games"
        const val STATUS_MONITOR = "$PREFIX/games/{id}/monitor"
        const val DELETE_MONITOR = "$PREFIX/games/{id}/monitor"
        const val GET_BY_ID = "$PREFIX/games/{id}"
        const val PLAY = "$PREFIX/games/{id}/play/row/{row}/column/{column}"
        const val GIVE_UP = "$PREFIX/games/{id}/giveup"

        fun variants() = URI(VARIANTS)
        fun variant(id: Int) = UriTemplate(VARIANT).expand(id)
        fun start() = URI(START)
        fun statusMonitor(id: UUID) = UriTemplate(STATUS_MONITOR).expand(id)
        fun statusMonitorTemplate() = UriTemplate(STATUS_MONITOR)
        fun byId(id: UUID) = UriTemplate(GET_BY_ID).expand(id)
        fun byIdTemplate() = UriTemplate(GET_BY_ID)
        fun playTemplate() = UriTemplate(PLAY)
        fun giveUp(id: UUID) = UriTemplate(GIVE_UP).expand(id)
    }
}
