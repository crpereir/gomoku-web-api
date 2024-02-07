package pt.isel.gomoku.http

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import pt.isel.gomoku.http.infra.Clazz
import pt.isel.gomoku.http.infra.HrefType.Href
import pt.isel.gomoku.http.infra.Rels
import pt.isel.gomoku.http.infra.siren
import pt.isel.gomoku.http.model.output.RSuccess
import pt.isel.gomoku.http.model.output.SystemInfoOutputModel
import pt.isel.gomoku.services.SystemService

@RestController
class SystemController(
    private val systemService: SystemService
) {

    /**
     * Retrieves the current system information.
     */
    @GetMapping(Uris.System.ABOUT)
    fun about(): ResponseEntity<*> {
        val res = systemService.getSystemDetails()
        checkNotNull(res.authors)
        return RSuccess.response(
            body = siren(SystemInfoOutputModel(res.version.toString(), res.createdAt.toString(), res.authors)) {
                clazz(Clazz.SYSTEM_INFO)
                clazz(Clazz.SYSTEM_AUTHORS)
                link(Href(Uris.System.about()), Rels.SELF)
            }
        )
    }
}
