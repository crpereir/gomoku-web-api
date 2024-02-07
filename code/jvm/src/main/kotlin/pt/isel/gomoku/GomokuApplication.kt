package pt.isel.gomoku

import kotlinx.datetime.Clock
import org.jdbi.v3.core.Jdbi
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import pt.isel.gomoku.domain.games.GamesDomainConfig
import pt.isel.gomoku.domain.token.Sha256TokenEncoder
import pt.isel.gomoku.domain.users.UsersDomainConfig
import pt.isel.gomoku.http.pipeline.AuthenticatedUserArgumentResolver
import pt.isel.gomoku.http.pipeline.AuthenticationInterceptor
import pt.isel.gomoku.repository.jdbi.configureWithAppRequirements
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@SpringBootApplication
class GomokuApplication {
    @Bean
    fun jdbi() = Jdbi.create(
        PGSimpleDataSource().apply {
            setURL(Environment.getDbUrl())
        }
    ).configureWithAppRequirements()

    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()

    @Bean
    fun usersDomainConfig() = UsersDomainConfig(
        tokenSizeInBytes = 256 / 8,
        tokenTtl = 24.hours,
        tokenRollingTtl = 1.hours,
        maxTokensPerUser = 1,
        limitUsersRanking = 10
    )

    @Bean
    fun gamesDomainConfig() = GamesDomainConfig(
        waitingTimeInWaitingLobby = 1.hours,
        playingRoundTime = 5.minutes
    )

    @Bean
    fun tokenEncoder() = Sha256TokenEncoder()

    @Bean
    fun clock() = Clock.System
}

@Configuration
class PipelineConfigurer(
    val authenticationInterceptor: AuthenticationInterceptor,
    val authenticatedUserArgumentResolver: AuthenticatedUserArgumentResolver
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(authenticationInterceptor)
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authenticatedUserArgumentResolver)
    }
}

// Entry point of application.
fun main(args: Array<String>) {
    runApplication<GomokuApplication>(*args)
}
