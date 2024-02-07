package pt.isel.gomoku.http.infra

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.HttpMethod
import org.springframework.web.util.UriTemplate
import pt.isel.gomoku.http.model.output.AuthenticationType
import java.net.URI

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SirenModel<T>(
    @get:JsonProperty("class")
    val clazz: List<String>,
    val properties: T?,
    val links: List<LinkModel>,
    val entities: List<EntityModel<*>>,
    val actions: List<ActionModel>
)

data class LinkModel(
    val rel: List<String>,
    val href: String,
    val authenticationType: List<String>
)

data class EntityModel<T>(
    val properties: T,
    val links: List<LinkModel>,
    val rel: List<String>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ActionModel(
    val name: String,
    val title: String?,
    val href: String,
    val method: String,
    val authenticationType: List<String>,
    val type: String?,
    val fields: List<FieldModel>
)

data class FieldModel(
    val name: String,
    val type: String,
    val value: String? = null
)

class SirenBuilderScope<T>(
    private val properties: T
) {
    private val links = mutableListOf<LinkModel>()
    private val entities = mutableListOf<EntityModel<*>>()
    private val classes = mutableListOf<String>()
    private val actions = mutableListOf<ActionModel>()

    fun clazz(value: String) {
        classes.add(value)
    }

    fun link(
        href: HrefType,
        rel: LinkRelation,
        authenticationType: List<AuthenticationType> = listOf(AuthenticationType.NONE)
    ) {
        links.add(
            LinkModel(
                listOf(rel.value),
                when (href) {
                    is HrefType.Href -> href.uri.toASCIIString()
                    is HrefType.HrefTemplate -> href.uriTemplate.toString()
                },
                authenticationType.map { auth -> auth.toString() }
            )
        )
    }

    fun <U> entity(value: U, rel: LinkRelation, block: EntityBuilderScope<U>.() -> Unit) {
        val scope = EntityBuilderScope(value, listOf(rel.value))
        scope.block()
        entities.add(scope.build())
    }

    fun action(
        name: String,
        href: HrefType,
        method: HttpMethod,
        type: String? = null,
        title: String? = null,
        authenticationType: List<AuthenticationType> = listOf(AuthenticationType.NONE),
        block: ActionBuilderScope.() -> Unit
    ) {
        val scope = ActionBuilderScope(name, title, href, method, type, authenticationType)
        scope.block()
        actions.add(scope.build())
    }

    fun build(): SirenModel<T> = SirenModel(
        clazz = classes,
        properties = properties,
        links = links,
        entities = entities,
        actions = actions
    )
}

class EntityBuilderScope<T>(
    private val properties: T,
    private val rel: List<String>

) {
    private val links = mutableListOf<LinkModel>()

    fun link(href: HrefType, rel: LinkRelation, authenticationType: List<AuthenticationType> = listOf(AuthenticationType.NONE)) {
        links.add(
            LinkModel(
                listOf(rel.value),
                when (href) {
                    is HrefType.Href -> href.uri.toASCIIString()
                    is HrefType.HrefTemplate -> href.uriTemplate.toString()
                },
                authenticationType.map { auth -> auth.toString() }
            )
        )
    }

    fun build(): EntityModel<T> = EntityModel(
        properties = properties,
        links = links,
        rel = rel
    )
}

class ActionBuilderScope(
    private val name: String,
    private val title: String?,
    private val href: HrefType,
    private val method: HttpMethod,
    private val type: String?,
    private val authenticationType: List<AuthenticationType>
) {
    private val fields = mutableListOf<FieldModel>()

    fun textField(name: String) {
        fields.add(FieldModel(name, "text"))
    }

    fun numberField(name: String) {
        fields.add(FieldModel(name, "number"))
    }

    fun hiddenField(name: String, value: String) {
        fields.add(FieldModel(name, "hidden", value))
    }

    fun build() = ActionModel(
        name,
        title,
        when (href) {
            is HrefType.Href -> href.uri.toASCIIString()
            is HrefType.HrefTemplate -> href.uriTemplate.toString()
        },
        method.name(),
        authenticationType.map { auth -> auth.toString() },
        type,
        fields
    )
}

fun <T> siren(value: T, block: SirenBuilderScope<T>.() -> Unit): SirenModel<T> {
    val scope = SirenBuilderScope(value)
    scope.block()
    return scope.build()
}

sealed class HrefType {
    class Href(val uri: URI) : HrefType()
    class HrefTemplate(val uriTemplate: UriTemplate) : HrefType()
}
