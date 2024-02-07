package pt.isel.gomoku.http

import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import pt.isel.gomoku.http.infra.Action
import pt.isel.gomoku.http.infra.Clazz
import pt.isel.gomoku.http.infra.HrefType.Href
import pt.isel.gomoku.http.infra.HrefType.HrefTemplate
import pt.isel.gomoku.http.infra.Rels
import pt.isel.gomoku.http.infra.siren
import pt.isel.gomoku.http.model.output.AuthenticationType
import pt.isel.gomoku.http.model.output.HomeOutputModel
import pt.isel.gomoku.http.model.output.RSuccess

@RestController
class HomeController {

    @GetMapping(Uris.Home.HOME)
    fun home(): ResponseEntity<*> {
        return RSuccess.response(
            body = siren(HomeOutputModel()) {
                clazz(Clazz.HOME)
                // Links with direct navigation
                link(Href(Uris.Home.home()), Rels.SELF)
                link(Href(Uris.System.about()), Rels.ABOUT)
                link(Href(Uris.Users.ranking()), Rels.RANKING)
                // Links without direct navigation, used to learn the front-end.
                link(HrefTemplate(Uris.Users.statisticsTemplate()), Rels.STATISTICS)
                link(Href(Uris.Users.home()), Rels.USER_HOME, AuthenticationType.needAuthentication())
                link(HrefTemplate(Uris.Games.byIdTemplate()), Rels.GAME, AuthenticationType.needAuthentication())
                link(HrefTemplate(Uris.Games.statusMonitorTemplate()), Rels.MONITOR, AuthenticationType.needAuthentication())
                action(
                    name = Action.Name.SIGNUP,
                    title = Action.Title.SIGNUP,
                    href = Href(Uris.Users.signup()),
                    method = HttpMethod.POST,
                    type = Action.Type.APPLICATION_JSON
                ) {
                    textField(Action.Field.USERNAME)
                    textField(Action.Field.EMAIL)
                    textField(Action.Field.PASSWORD)
                }
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
    }
}
