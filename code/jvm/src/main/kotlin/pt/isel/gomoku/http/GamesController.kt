package pt.isel.gomoku.http

import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pt.isel.gomoku.domain.games.GameError.GamePlayError.GameAlreadyEnded
import pt.isel.gomoku.domain.games.GameError.GamePlayError.NotYourTurn
import pt.isel.gomoku.domain.games.GameError.GamePlayError.PositionNotAvailable
import pt.isel.gomoku.domain.games.GameError.GamePlayError.PositionNotValid
import pt.isel.gomoku.domain.games.GameError.GamePlayError.TimeOut
import pt.isel.gomoku.domain.games.GameError.GetGameError.GameNotFound
import pt.isel.gomoku.domain.games.GameError.GetGameError.PlayerNotPartOfGame
import pt.isel.gomoku.domain.games.GiveUpError
import pt.isel.gomoku.domain.games.StartResultError
import pt.isel.gomoku.domain.games.StatusMonitor
import pt.isel.gomoku.domain.users.AuthenticatedUser
import pt.isel.gomoku.http.infra.Action
import pt.isel.gomoku.http.infra.Clazz
import pt.isel.gomoku.http.infra.HrefType.Href
import pt.isel.gomoku.http.infra.HrefType.HrefTemplate
import pt.isel.gomoku.http.infra.Rels
import pt.isel.gomoku.http.infra.siren
import pt.isel.gomoku.http.model.input.VariantInputModel
import pt.isel.gomoku.http.model.output.AuthenticationType
import pt.isel.gomoku.http.model.output.GameOutputModel
import pt.isel.gomoku.http.model.output.Problem
import pt.isel.gomoku.http.model.output.RSuccess
import pt.isel.gomoku.http.model.output.StatusMonitorOutputModel
import pt.isel.gomoku.http.model.output.VariantInfo
import pt.isel.gomoku.http.model.output.VariantOutputModel
import pt.isel.gomoku.repository.jdbi.StartStatus
import pt.isel.gomoku.services.GamesService
import pt.isel.gomoku.utils.Failure
import pt.isel.gomoku.utils.Success
import java.util.*

/**
 * Represents the default values of GamesController.
 */
object DefaultGamesValues {
    const val VARIANT_ID = 1
}

@RestController
class GamesController(
    private val gameService: GamesService
) {

    /**
     * Obtains all the known game variants.
     * NOTE: Pagination are not used due to a small amount of known variations. These should be used
     * if the number of variations increases.
     */
    @GetMapping(Uris.Games.VARIANTS)
    fun variants(user: AuthenticatedUser): ResponseEntity<*> {
        val res = gameService.variants()
        val info = res.map {
            VariantInfo(
                it.id,
                it.boardDim,
                it.openingRules.toString(),
                it.playingRules.toString()
            )
        }
        return RSuccess.response(
            body = siren(VariantOutputModel(info)) {
                clazz(Clazz.VARIANTS)
                link(Href(Uris.Games.variants()), Rels.SELF, AuthenticationType.needAuthentication())
                action(
                    name = Action.Name.START,
                    title = Action.Title.START,
                    href = Href(Uris.Games.start()),
                    method = HttpMethod.POST,
                    authenticationType = AuthenticationType.needAuthentication(),
                    type = Action.Type.APPLICATION_JSON
                ) {
                    numberField(Action.Field.VARIANT_ID)
                }
            }
        )
    }

    /**
     * Obtains the variant with a given ID.
     */
    @GetMapping(Uris.Games.VARIANT)
    fun variant(user: AuthenticatedUser, @PathVariable id: Int): ResponseEntity<*> {
        return when (val res = gameService.variant(id)) {
            is Success -> {
                val info = VariantInfo(
                    id,
                    res.value.boardDim,
                    res.value.openingRules.toString(),
                    res.value.playingRules.toString()
                )
                RSuccess.response(
                    body = siren(info) {
                        clazz(Clazz.VARIANT)
                        link(Href(Uris.Games.variant(id)), Rels.SELF, AuthenticationType.needAuthentication())
                    }
                )
            }
            is Failure ->
                Problem.response(
                    400,
                    Problem.Type.INVALID_VARIANT,
                    Problem.Title.INVALID_VARIANT
                )
        }
    }

    /**
     *  Starting a new game.
     *  If none are waiting, this start is "delayed" until another player enters.
     */
    @PostMapping(Uris.Games.START)
    fun start(user: AuthenticatedUser, @RequestBody input: VariantInputModel?): ResponseEntity<*> {
        val variantId = input?.variantId ?: DefaultGamesValues.VARIANT_ID

        return when (val res = gameService.start(user.user.id, variantId)) {
            is Success -> when (res.value) {
                is StartStatus.Register -> RSuccess.response(
                    status = 202,
                    body = siren(
                        StatusMonitorOutputModel(
                            status = StatusMonitorOutputModel.STATUS_WAITING,
                            askAgainIn = StatusMonitorOutputModel.ASK_AGAIN_IN
                        )
                    ) {
                        clazz(Clazz.MONITOR)
                        link(
                            Href(Uris.Games.statusMonitor(res.value.id)),
                            Rels.SELF,
                            AuthenticationType.needAuthentication()
                        )
                        action(
                            name = Action.Name.CANCEL_START,
                            title = Action.Title.CANCEL_START,
                            href = Href(Uris.Games.statusMonitor(res.value.id)),
                            method = HttpMethod.DELETE,
                            authenticationType = AuthenticationType.needAuthentication()
                        ) { }
                    }
                )

                is StartStatus.Create -> RSuccess.response(
                    status = 201,
                    headers = hashMapOf("Location" to Uris.Games.byId(res.value.id).toASCIIString()),
                    body = siren(null) {
                        link(Href(Uris.Games.byId(res.value.id)), Rels.SELF, AuthenticationType.needAuthentication())
                        action(
                            name = Action.Name.PLAY,
                            title = Action.Title.PLAY,
                            href = HrefTemplate(Uris.Games.playTemplate()),
                            method = HttpMethod.PUT,
                            authenticationType = AuthenticationType.needAuthentication()
                        ) { }
                        action(
                            name = Action.Name.GIVE_UP,
                            title = Action.Title.GIVE_UP,
                            href = Href(Uris.Games.giveUp(res.value.id)),
                            method = HttpMethod.PUT,
                            authenticationType = AuthenticationType.needAuthentication()
                        ) { }
                    }
                )
            }

            is Failure -> when (res.value) {
                is StartResultError.PlayerAlreadyWaiting -> Problem.response(
                    403,
                    Problem.Type.PLAYER_ALREADY_WAITING,
                    Problem.Title.PLAYER_ALREADY_WAITING
                )

                is StartResultError.VariantInvalid -> Problem.response(
                    400,
                    Problem.Type.INVALID_VARIANT,
                    Problem.Title.INVALID_VARIANT
                )
            }
        }
    }

    /**
     * Checks the current state of game request.
     */
    @GetMapping(Uris.Games.STATUS_MONITOR)
    fun statusMonitor(user: AuthenticatedUser, @PathVariable id: UUID): ResponseEntity<*> {
        return when (val res = gameService.statusMonitor(user.user.id, id)) {
            is Success -> when (res.value) {
                is StatusMonitor.StillWaiting -> RSuccess.response(
                    body = siren(
                        StatusMonitorOutputModel(
                            status = StatusMonitorOutputModel.STATUS_WAITING,
                            askAgainIn = StatusMonitorOutputModel.ASK_AGAIN_IN
                        )
                    ) {
                        clazz(Clazz.MONITOR)
                        link(Href(Uris.Games.statusMonitor(id)), Rels.SELF, AuthenticationType.needAuthentication())
                        action(
                            name = Action.Name.CANCEL_START,
                            title = Action.Title.CANCEL_START,
                            href = Href(Uris.Games.statusMonitor(id)),
                            method = HttpMethod.DELETE,
                            authenticationType = AuthenticationType.needAuthentication()
                        ) { }
                    }
                )

                is StatusMonitor.Started -> RSuccess.response(
                    body = siren(
                        StatusMonitorOutputModel(
                            status = StatusMonitorOutputModel.STATUS_JOINED
                        )
                    ) {
                        clazz(Clazz.MONITOR)
                        link(
                            Href(Uris.Games.byId(res.value.gameId)),
                            Rels.SELF,
                            AuthenticationType.needAuthentication()
                        )
                        action(
                            name = Action.Name.PLAY,
                            title = Action.Title.PLAY,
                            href = HrefTemplate(Uris.Games.playTemplate()),
                            method = HttpMethod.PUT,
                            authenticationType = AuthenticationType.needAuthentication()
                        ) { }
                        action(
                            name = Action.Name.GIVE_UP,
                            title = Action.Title.GIVE_UP,
                            href = Href(Uris.Games.giveUp(res.value.gameId)),
                            method = HttpMethod.PUT,
                            authenticationType = AuthenticationType.needAuthentication()
                        ) { }
                    }
                )
            }

            is Failure -> Problem.response(
                404,
                Problem.Type.MONITOR_NOT_FOUND,
                Problem.Title.MONITOR_NOT_FOUND
            )
        }
    }

    /**
     * Gives up on waiting for the game.
     */
    @DeleteMapping(Uris.Games.DELETE_MONITOR)
    fun deleteMonitor(user: AuthenticatedUser, @PathVariable id: UUID): ResponseEntity<*> {
        return when (gameService.deleteMonitor(user.user.id, id)) {
            is Success ->
                ResponseEntity
                    .status(200)
                    .build<Unit>()

            is Failure -> Problem.response(
                404,
                Problem.Type.MONITOR_NOT_FOUND,
                Problem.Title.MONITOR_NOT_FOUND
            )
        }
    }

    /**
     * Retrieves the game state of game with given ID.
     */
    @GetMapping(Uris.Games.GET_BY_ID)
    fun getById(user: AuthenticatedUser, @PathVariable id: UUID): ResponseEntity<*> {
        return when (val res = gameService.getById(user.user.id, id)) {
            is Success -> RSuccess.response(
                body = siren(
                    GameOutputModel(
                        res.value.gameState,
                        res.value.playerB,
                        res.value.playerW,
                        res.value.playingRoundTime.toString(),
                        res.value.pieces,
                        res.value.variantId

                    )
                ) {
                    clazz(Clazz.GAME)
                    link(Href(Uris.Games.byId(id)), Rels.SELF, AuthenticationType.needAuthentication())
                    link(Href(Uris.Games.variant(res.value.variantId)), Rels.VARIANT, AuthenticationType.needAuthentication())
                    action(
                        name = Action.Name.PLAY,
                        title = Action.Title.PLAY,
                        href = HrefTemplate(Uris.Games.playTemplate()),
                        method = HttpMethod.PUT,
                        authenticationType = AuthenticationType.needAuthentication()
                    ) { }
                    action(
                        name = Action.Name.GIVE_UP,
                        title = Action.Title.GIVE_UP,
                        href = Href(Uris.Games.giveUp(id)),
                        method = HttpMethod.PUT,

                        authenticationType = AuthenticationType.needAuthentication()
                    ) { }
                }
            )

            is Failure ->
                when (res.value) {
                    is PlayerNotPartOfGame -> Problem.response(
                        403,
                        Problem.Type.PLAYER_NOT_PART_OF_GAME,
                        Problem.Title.PLAYER_NOT_PART_OF_GAME
                    )

                    is GameNotFound -> Problem.response(
                        404,
                        Problem.Type.GAME_NOT_FOUND,
                        Problem.Title.GAME_NOT_FOUND,
                        detail = "Game with id '$id' not found."
                    )
                }
        }
    }

    /**
     * Makes a move in game with given ID.
     */
    @PutMapping(Uris.Games.PLAY)
    fun play(
        user: AuthenticatedUser,
        @PathVariable id: UUID,
        @PathVariable row: Int,
        @PathVariable column: Int
    ): ResponseEntity<*> {
        return when (val res = gameService.play(user.user.id, id, row, column)) {
            is Success -> RSuccess.response(
                body = siren(
                    GameOutputModel(
                        res.value.gameState,
                        res.value.playerB,
                        res.value.playerW,
                        res.value.playingRoundTime.toString(),
                        res.value.pieces,
                        res.value.variantId
                    )
                ) {
                    clazz(Clazz.GAME)
                    link(Href(Uris.Games.byId(id)), Rels.SELF, AuthenticationType.needAuthentication())
                    action(
                        name = Action.Name.PLAY,
                        title = Action.Title.PLAY,
                        href = HrefTemplate(Uris.Games.playTemplate()),
                        method = HttpMethod.PUT,
                        authenticationType = AuthenticationType.needAuthentication()
                    ) { }
                    action(
                        name = Action.Name.GIVE_UP,
                        title = Action.Title.GIVE_UP,
                        href = Href(Uris.Games.giveUp(id)),
                        method = HttpMethod.PUT,
                        authenticationType = AuthenticationType.needAuthentication()
                    ) { }
                }
            )

            is Failure ->
                when (res.value) {
                    is TimeOut ->
                        Problem.response(
                            408,
                            Problem.Type.TIME_OUT,
                            Problem.Title.TIME_OUT,
                            Problem.Detail.TIME_OUT_PLAYING
                        )

                    is PlayerNotPartOfGame ->
                        Problem.response(
                            403,
                            Problem.Type.PLAYER_NOT_PART_OF_GAME,
                            Problem.Title.PLAYER_NOT_PART_OF_GAME
                        )

                    is GameNotFound ->
                        Problem.response(
                            404,
                            Problem.Type.GAME_NOT_FOUND,
                            Problem.Title.GAME_NOT_FOUND
                        )

                    is PositionNotAvailable ->
                        Problem.response(
                            400,
                            Problem.Type.MOVE_NOT_VALID,
                            Problem.Title.MOVE_NOT_VALID,
                            "There is already a piece in that position."
                        )

                    is NotYourTurn ->
                        Problem.response(
                            400,
                            Problem.Type.NOT_YOUR_TURN,
                            Problem.Title.NOT_YOUR_TURN
                        )

                    is GameAlreadyEnded ->
                        Problem.response(
                            400,
                            Problem.Type.GAME_ALREADY_ENDED,
                            Problem.Title.GAME_ALREADY_ENDED
                        )

                    is PositionNotValid ->
                        Problem.response(
                            400,
                            Problem.Type.MOVE_NOT_VALID,
                            Problem.Title.MOVE_NOT_VALID,
                            "Position indicated is not valid."
                        )
                }
        }
    }

    /**
     * Giving up on game with given game ID.
     */
    @PutMapping(Uris.Games.GIVE_UP)
    fun giveUp(user: AuthenticatedUser, @PathVariable id: UUID): ResponseEntity<*> {
        return when (val res = gameService.giveUp(user.user.id, id)) {
            is Success ->
                ResponseEntity
                    .status(200)
                    .build<Unit>()

            is Failure ->
                when (res.value) {
                    is GiveUpError.PlayerNotPartOfGame -> Problem.response(
                        403,
                        Problem.Type.PLAYER_NOT_PART_OF_GAME,
                        Problem.Title.PLAYER_NOT_PART_OF_GAME
                    )

                    is GiveUpError.GameNotFound -> Problem.response(
                        404,
                        Problem.Type.GAME_NOT_FOUND,
                        Problem.Title.GAME_NOT_FOUND,
                        detail = "Game with id '$id' not found."
                    )

                    is GiveUpError.GameAlreadyEnded -> Problem.response(
                        400,
                        Problem.Type.GAME_ALREADY_ENDED,
                        Problem.Title.GAME_ALREADY_ENDED
                    )
                }
        }
    }
}
