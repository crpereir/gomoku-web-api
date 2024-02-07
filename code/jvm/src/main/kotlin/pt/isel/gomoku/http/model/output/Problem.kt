package pt.isel.gomoku.http.model.output

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.http.ResponseEntity
import java.net.URI

@JsonInclude(JsonInclude.Include.NON_NULL)
class Problem(
    val type: URI,
    val title: String? = null,
    val detail: String? = null,
    val instance: URI? = null
) {

    companion object {

        const val DOC_LOCATION = "https://github.com/isel-leic-daw/2023-daw-leic51d-07/tree/main/docs/api/problem/"
        private const val MEDIA_TYPE = "application/problem+json"

        fun response(
            status: Int,
            type: URI,
            title: String? = null,
            detail: String? = null,
            instance: URI? = null
        ): ResponseEntity<Any> {
            return ResponseEntity
                .status(status)
                .header("Content-Type", MEDIA_TYPE)
                .body(Problem(type, title, detail, instance))
        }
    }

    object Type {

        val INTERNAL_SERVER_ERROR = URI(DOC_LOCATION + "internalServerError")
        val INVALID_REQUEST_CONTENT = URI(DOC_LOCATION + "invalidRequestContent")

        val USER_NOT_FOUND = URI(DOC_LOCATION + "userNotFound")
        val USER_ALREADY_EXISTS = URI(DOC_LOCATION + "userAlreadyExists")
        val INSECURE_PASSWORD = URI(DOC_LOCATION + "insecurePassword")
        val EMAIL_NOT_VALID = URI(DOC_LOCATION + "emailNotValid")
        val INVALID_CREDENTIALS = URI(DOC_LOCATION + "invalidCredentials")
        val PLAYER_ALREADY_WAITING = URI(DOC_LOCATION + "playerAlreadyWaiting")

        val INVALID_VARIANT = URI(DOC_LOCATION + "invalidVariant")
        val MONITOR_NOT_FOUND = URI(DOC_LOCATION + "monitorNotFound")
        val GAME_NOT_FOUND = URI(DOC_LOCATION + "gameNotFound")
        val PLAYER_NOT_PART_OF_GAME = URI(DOC_LOCATION + "playerNotPartOfGame")
        val MOVE_NOT_VALID = URI(DOC_LOCATION + "moveNotValid")
        val GAME_ALREADY_ENDED = URI(DOC_LOCATION + "gameAlreadyEnded")
        val NOT_YOUR_TURN = URI(DOC_LOCATION + "notYourTurn")

        val TIME_OUT = URI(DOC_LOCATION + "timeOut")
    }

    object Title {
        const val INTERNAL_SERVER_ERROR = "Unknown error occurred."
        const val INVALID_REQUEST_CONTENT = "Invalid request content."

        const val USER_NOT_FOUND = "User not found."
        const val USER_ALREADY_EXISTS = "User already exists."
        const val INSECURE_PASSWORD = "Password not secure."
        const val EMAIL_NOT_VALID = "Email not valid."
        const val INVALID_CREDENTIALS = "Credentials are invalid."

        const val INVALID_VARIANT = "Variant is invalid."
        const val MONITOR_NOT_FOUND = "Monitor not found."
        const val GAME_NOT_FOUND = "Game not found."
        const val PLAYER_NOT_PART_OF_GAME = "You are not a player in this game."
        const val MOVE_NOT_VALID = "Move invalid."
        const val GAME_ALREADY_ENDED = "Game ended."
        const val NOT_YOUR_TURN = "It is not your turn yet."
        const val PLAYER_ALREADY_WAITING = "You are already waiting."

        const val TIME_OUT = "Deadline reached."
    }

    object Detail {
        const val INTERNAL_SERVER_ERROR =
            "Sorry! Something went wrong. Please try again later."

        const val INSECURE_PASSWORD =
            "Password must have at least 6 characters, a lowercase letter, an uppercase letter and a digit."

        const val TIME_OUT_WAITING =
            "Could not find a game. Try again."

        const val TIME_OUT_PLAYING =
            "You took too much time. You lost."
    }
}
