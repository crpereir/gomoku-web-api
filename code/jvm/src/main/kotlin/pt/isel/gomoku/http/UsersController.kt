package pt.isel.gomoku.http

import kotlinx.datetime.Clock
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pt.isel.gomoku.domain.token.TokenCreationError
import pt.isel.gomoku.domain.users.AuthenticatedUser
import pt.isel.gomoku.domain.users.Page
import pt.isel.gomoku.domain.users.UserCreationError
import pt.isel.gomoku.http.infra.Action
import pt.isel.gomoku.http.infra.Clazz
import pt.isel.gomoku.http.infra.HrefType.Href
import pt.isel.gomoku.http.infra.Rels
import pt.isel.gomoku.http.infra.Rels.FIRST
import pt.isel.gomoku.http.infra.Rels.LAST
import pt.isel.gomoku.http.infra.Rels.NEXT
import pt.isel.gomoku.http.infra.Rels.PREVIOUS
import pt.isel.gomoku.http.infra.siren
import pt.isel.gomoku.http.model.input.UserCreateInputModel
import pt.isel.gomoku.http.model.input.UserCreateTokenInputModel
import pt.isel.gomoku.http.model.output.AuthenticationType
import pt.isel.gomoku.http.model.output.Problem
import pt.isel.gomoku.http.model.output.RSuccess
import pt.isel.gomoku.http.model.output.UserHomeOutputModel
import pt.isel.gomoku.http.model.output.UserOutputModel
import pt.isel.gomoku.http.model.output.UserStatisticsInfo
import pt.isel.gomoku.http.model.output.UserStatisticsOutputModel
import pt.isel.gomoku.http.model.output.UserTokenCreateOutputModel
import pt.isel.gomoku.http.model.output.generateSessionCookies
import pt.isel.gomoku.services.UsersService
import pt.isel.gomoku.utils.Failure
import pt.isel.gomoku.utils.Success
import kotlin.time.Duration.Companion.hours

/**
 * Represents the default values of UsersController.
 */
object DefaultUsersValues {
    const val RANKING_FIRST_PAGE = 1
}

@RestController
class UsersController(
    private val userService: UsersService
) {

    /**
     * Creating a new user through sign-up.
     */
    @PostMapping(Uris.Users.CREATE)
    fun create(@RequestBody input: UserCreateInputModel): ResponseEntity<*> {
        return when (val res = userService.createUser(input.username, input.email, input.password)) {
            is Success -> RSuccess.response(
                status = 201,
                headers = hashMapOf("Location" to Uris.Users.byId(res.value).toASCIIString()),
                body = siren(null) {
                    link(Href(Uris.Users.byId(res.value)), Rels.SELF)
                    action(
                        name = Action.Name.LOGIN,
                        title = Action.Title.LOGIN,
                        href = Href(Uris.Users.login()),
                        method = HttpMethod.POST,
                        type = Action.Type.APPLICATION_JSON
                    ) {
                        textField(Action.Field.IDENTITY)
                        textField(Action.Field.PASSWORD)
                    }
                }
            )

            is Failure -> when (res.value) {
                UserCreationError.UserAlreadyExists -> Problem.response(
                    400,
                    Problem.Type.USER_ALREADY_EXISTS,
                    Problem.Title.USER_ALREADY_EXISTS,
                    detail = "User with username '${input.username}' and email '${input.email}' already exists."
                )

                UserCreationError.EmailNotValid -> Problem.response(
                    400,
                    Problem.Type.EMAIL_NOT_VALID,
                    Problem.Title.EMAIL_NOT_VALID,
                    detail = "Email '${input.email}' does not comply with the standard email format."
                )

                UserCreationError.InsecurePassword -> Problem.response(
                    400,
                    Problem.Type.INSECURE_PASSWORD,
                    Problem.Title.INSECURE_PASSWORD,
                    Problem.Detail.INSECURE_PASSWORD
                )
            }
        }
    }

    /**
     * Obtaining a paginated ranking.
     */
    @GetMapping(Uris.Users.RANKING)
    fun ranking(@RequestParam page: Int?): ResponseEntity<*> {
        val res = userService.getUsersRanking(page ?: DefaultUsersValues.RANKING_FIRST_PAGE)
        val listUsers = res.ranking.map {
            UserStatisticsInfo(
                it.userId,
                it.username,
                it.rankingPosition,
                it.gamesPlayed,
                it.gamesWon,
                it.gamesLost,
                it.gamesTied
            )
        }
        return RSuccess.response(
            body = siren(UserStatisticsOutputModel(res.totalUsers, listUsers)) {
                clazz(Clazz.RANKING)
                link(Href(Uris.Users.ranking()), Rels.SELF)
                listUsers.forEach {
                    entity("user-${it.userId}-statistics", Rels.STATISTICS) {
                        link(Href(Uris.Users.statistics(it.userId)), Rels.SELF)
                    }
                }
                when (val pageType = res.page) {
                    is Page.First -> {
                        link(Href(Uris.Users.ranking(pageType.next)), NEXT)
                        link(Href(Uris.Users.ranking(pageType.last)), LAST)
                    }
                    is Page.Last -> {
                        link(Href(Uris.Users.ranking(pageType.previous)), PREVIOUS)
                        link(Href(Uris.Users.ranking(pageType.first)), FIRST)
                    }
                    is Page.Middle -> {
                        link(Href(Uris.Users.ranking(pageType.previous)), PREVIOUS)
                        link(Href(Uris.Users.ranking(pageType.next)), NEXT)
                        link(Href(Uris.Users.ranking(pageType.first)), FIRST)
                        link(Href(Uris.Users.ranking(pageType.last)), LAST)
                    }
                    is Page.Single -> { }
                }
            }
        )
    }

    /**
     * Obtaining a user with given ID.
     */
    @GetMapping(Uris.Users.GET_BY_ID)
    fun getById(@PathVariable id: Int): ResponseEntity<*> {
        return when (val res = userService.getUserById(id)) {
            is Success -> RSuccess.response(
                body = siren(UserOutputModel(res.value.username, res.value.email)) {
                    clazz(Clazz.USER)
                    link(Href(Uris.Users.byId(id)), Rels.SELF)
                }
            )

            is Failure -> Problem.response(
                404,
                Problem.Type.USER_NOT_FOUND,
                Problem.Title.USER_NOT_FOUND,
                detail = "User with id $id not found."
            )
        }
    }

    /**
     * Obtaining the statistics of only one user.
     */
    @GetMapping(Uris.Users.STATISTICS)
    fun statistics(@PathVariable id: Int): ResponseEntity<*> {
        return when (val res = userService.getUserStatistics(id)) {
            is Success -> {
                val info = UserStatisticsInfo(
                    res.value.userId,
                    res.value.username,
                    res.value.rankingPosition,
                    res.value.gamesPlayed,
                    res.value.gamesWon,
                    res.value.gamesLost,
                    res.value.gamesTied
                )
                RSuccess.response(
                    body = siren(info) {
                        clazz(Clazz.STATISTICS)
                        link(Href(Uris.Users.statistics(id)), Rels.SELF)
                    }
                )
            }

            is Failure -> Problem.response(
                404,
                Problem.Type.USER_NOT_FOUND,
                Problem.Title.USER_NOT_FOUND,
                detail = "User with id $id not found."
            )
        }
    }

    /**
     * Obtaining a token through login.
     */
    @PostMapping(Uris.Users.TOKEN)
    fun token(@RequestBody input: UserCreateTokenInputModel): ResponseEntity<*> {
        return when (val res = userService.createToken(input.identity, input.password)) {
            is Success ->
                RSuccess.response(
                    body = siren(UserTokenCreateOutputModel(res.value.tokenValue, res.value.tokenExpiration.toString())) {
                        clazz(Clazz.AUTHORIZATION)
                        link(Href(Uris.Users.home()), Rels.USER_HOME, AuthenticationType.needAuthentication())
                    },
                    cookies = generateSessionCookies(res.value.userId, res.value.username, res.value.tokenValue, res.value.tokenExpiration)
                )

            is Failure -> when (res.value) {
                TokenCreationError.InvalidCredentials ->
                    Problem.response(
                        400,
                        Problem.Type.INVALID_CREDENTIALS,
                        Problem.Title.INVALID_CREDENTIALS
                    )
            }
        }
    }

    /**
     * Logging out, invalidating token and cookie through setting expired cookie.
     */
    @PostMapping(Uris.Users.LOGOUT)
    fun logout(user: AuthenticatedUser): ResponseEntity<*> {
        userService.revokeToken(user.token)
        val cookies = generateSessionCookies(user.user.id, user.user.username, user.token, Clock.System.now().minus(1.hours))
        val responseEntity = ResponseEntity
            .status(200)
            .header("Content-Type", "application/json")

        cookies.forEach { cookie ->
            responseEntity.header("Set-Cookie", cookie)
        }
        return responseEntity.build<Unit>()
    }

    /**
     * Requesting information of user.
     */
    @GetMapping(Uris.Users.USER_HOME)
    fun getUserHome(userAuthenticatedUser: AuthenticatedUser): ResponseEntity<*> {
        return RSuccess.response(
            body = siren(
                UserHomeOutputModel(
                    userId = userAuthenticatedUser.user.id,
                    username = userAuthenticatedUser.user.username,
                    email = userAuthenticatedUser.user.email
                )

            ) {
                clazz(Clazz.USER_HOME)
                link(Href(Uris.Users.home()), Rels.SELF, AuthenticationType.needAuthentication())
                link(Href(Uris.Games.variants()), Rels.VARIANTS, AuthenticationType.needAuthentication())
                action(
                    name = Action.Name.START,
                    title = Action.Title.START,
                    href = Href(Uris.Games.start()),
                    method = HttpMethod.POST,
                    authenticationType = AuthenticationType.needAuthentication()
                ) {
                    numberField(Action.Field.VARIANT_ID)
                }
                action(
                    name = Action.Name.LOGOUT,
                    title = Action.Title.LOGOUT,
                    href = Href(Uris.Users.logout()),
                    method = HttpMethod.POST,
                    authenticationType = AuthenticationType.needAuthentication()
                ) { }
            }
        )
    }
}
