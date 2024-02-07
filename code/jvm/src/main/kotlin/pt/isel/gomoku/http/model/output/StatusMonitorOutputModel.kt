package pt.isel.gomoku.http.model.output

import kotlin.time.Duration.Companion.seconds

data class StatusMonitorOutputModel(
    val status: StatusOutputModel,
    val askAgainIn: String? = null
) {
    companion object {
        val STATUS_WAITING = StatusOutputModel.WAITING_FOR_OTHER_PLAYER
        val STATUS_JOINED = StatusOutputModel.OTHER_PLAYER_JOINED

        val ASK_AGAIN_IN = 10.seconds.toString()
    }
}

enum class StatusOutputModel {
    WAITING_FOR_OTHER_PLAYER, OTHER_PLAYER_JOINED
}
