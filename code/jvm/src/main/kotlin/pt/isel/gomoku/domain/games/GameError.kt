package pt.isel.gomoku.domain.games

import pt.isel.gomoku.domain.games.GameError.GamePlayError
import pt.isel.gomoku.domain.games.GameError.GetGameError

/**
 * Represents several errors in Game-related operations.
 * Either in obtaining the game [GetGameError] or in trying to play [GamePlayError]
 */
sealed class GameError {

    /**
     * Errors that are made when obtaining a game from database.
     */
    sealed class GetGameError : GameError() {
        object GameNotFound : GetGameError()
        object PlayerNotPartOfGame : GetGameError()
    }

    /**
     * Errors that are linked to game logic.
     */
    sealed class GamePlayError : GameError() {
        object NotYourTurn : GamePlayError()
        object TimeOut : GamePlayError()
        object GameAlreadyEnded : GamePlayError()
        object PositionNotAvailable : GamePlayError()
        object PositionNotValid : GamePlayError()
    }
}

/**
 * Errors related to give up attempt.
 */
sealed class GiveUpError {
    object GameNotFound : GiveUpError()
    object PlayerNotPartOfGame : GiveUpError()
    object GameAlreadyEnded : GiveUpError()
}

/**
 * Errors related to start game attempt.
 */
sealed class StartResultError {
    object PlayerAlreadyWaiting : StartResultError()
    object VariantInvalid : StartResultError()
}

/**
 * Errors related to fetching a variant.
 */
sealed class VariantError {
    object VariantInvalid : VariantError()
}
