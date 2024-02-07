package pt.isel.gomoku.repository.jdbi

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.postgres.PostgresPlugin
import pt.isel.gomoku.repository.jdbi.argumentfactory.LocalDateArgumentFactory
import pt.isel.gomoku.repository.jdbi.argumentfactory.VersionArgumentFactory
import pt.isel.gomoku.repository.jdbi.mappers.BoardMapper
import pt.isel.gomoku.repository.jdbi.mappers.InstantMapper
import pt.isel.gomoku.repository.jdbi.mappers.LocalDateMapper
import pt.isel.gomoku.repository.jdbi.mappers.PasswordValidationInfoMapper
import pt.isel.gomoku.repository.jdbi.mappers.TokenValidationInfoMapper
import pt.isel.gomoku.repository.jdbi.mappers.VersionMapper

fun Jdbi.configureWithAppRequirements(): Jdbi {
    installPlugin(KotlinPlugin())
    installPlugin(PostgresPlugin())

    registerColumnMapper(PasswordValidationInfoMapper())
    registerColumnMapper(TokenValidationInfoMapper())
    registerColumnMapper(InstantMapper())
    registerColumnMapper(LocalDateMapper())
    registerColumnMapper(VersionMapper())
    registerColumnMapper(BoardMapper())

    registerArgument(VersionArgumentFactory())
    registerArgument(LocalDateArgumentFactory())

    return this
}
