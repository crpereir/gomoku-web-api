package pt.isel.gomoku.repository.jdbi

import kotlinx.datetime.Instant
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.slf4j.LoggerFactory
import pt.isel.gomoku.domain.token.Token
import pt.isel.gomoku.domain.token.TokenValidationInfo
import pt.isel.gomoku.domain.users.PasswordValidationInfo
import pt.isel.gomoku.domain.users.Ranking
import pt.isel.gomoku.domain.users.User
import pt.isel.gomoku.domain.users.UserStatistics
import pt.isel.gomoku.repository.UsersRepository

class JdbiUsersRepository(
    private val handle: Handle
) : UsersRepository {

    override fun isUserStoredByUsernameOrEmail(username: String, email: String): Boolean =
        handle.createQuery("select count(*) from dbo.Users where username = :username or email = :email")
            .bind("username", username)
            .bind("email", email)
            .mapTo<Int>()
            .single() == 1

    override fun storeUser(username: String, email: String, passwordValidation: PasswordValidationInfo): Int =
        handle.createUpdate(
            """
            insert into dbo.Users (username, email, password_validation) values (:username, :email, :password_validation)
            """
        )
            .bind("username", username)
            .bind("email", email)
            .bind("password_validation", passwordValidation.validationInfo)
            .executeAndReturnGeneratedKeys()
            .mapTo<Int>()
            .one()

    override fun getUsersRanking(skip: Int, limit: Int): Ranking =
        handle.createQuery(
            """
            select r.user_id, u.username, r.ranking_position, 
                (u.games_won + u.games_lost + u.games_tied) as games_played, u.games_won, u.games_lost, u.games_tied 
            from dbo.Ranking_View r join dbo.Users u 
            on u.id = r.user_id 
            order by r.ranking_position ASC
            offset :skip limit :limit
            """.trimIndent()
        )
            .bind("skip", skip)
            .bind("limit", limit)
            .mapTo<UserStatistics>()
            .list()

    override fun numberOfUsersInRanking(): Int? =
        handle.createQuery("select count(*) from dbo.Ranking_View").mapTo<Int>().one()

    override fun getUserById(userId: Int): User? =
        handle.createQuery("select id, username, email, password_validation from dbo.Users where id = :id")
            .bind("id", userId)
            .mapTo<User>()
            .singleOrNull()

    override fun getUserStatistics(userId: Int): UserStatistics? =
        handle.createQuery(
            """
            select r.user_id, u.username, r.ranking_position, 
                (u.games_won + u.games_lost + u.games_tied) as games_played, u.games_won, u.games_lost, u.games_tied 
            from dbo.Ranking_View r join dbo.Users u 
            on u.id = r.user_id 
            where u.id = :id;
            """
        )
            .bind("id", userId)
            .mapTo<UserStatistics>()
            .singleOrNull()

    override fun getUserByUsernameOrEmail(identity: String): User? {
        return handle.createQuery("select id, username, email, password_validation from dbo.Users where username = :username or email = :email")
            .bind("username", identity)
            .bind("email", identity)
            .mapTo<User>()
            .singleOrNull()
    }

    override fun createToken(token: Token, maxTokens: Int) {
        handle.createUpdate(
            """
            delete from dbo.Tokens 
            where user_id = :user_id 
                and token_validation in (
                    select token_validation from dbo.Tokens where user_id = :user_id 
                        order by last_used_at desc offset :offset
                )
            """.trimIndent()
        )
            .bind("user_id", token.userId)
            .bind("offset", maxTokens - 1)
            .execute()

        handle.createUpdate(
            """
                insert into dbo.Tokens(user_id, token_validation, created_at, last_used_at) 
                values (:user_id, :token_validation, :created_at, :last_used_at)
            """.trimIndent()
        )
            .bind("user_id", token.userId)
            .bind("token_validation", token.tokenValidationInfo.validationInfo)
            .bind("created_at", token.createdAt.epochSeconds)
            .bind("last_used_at", token.lastUsedAt.epochSeconds)
            .execute()
    }

    override fun updateTokenLastUsed(token: Token, now: Instant) {
        handle.createUpdate(
            """
                update dbo.Tokens
                set last_used_at = :last_used_at
                where token_validation = :validation_information
            """.trimIndent()
        )
            .bind("last_used_at", now.epochSeconds)
            .bind("validation_information", token.tokenValidationInfo.validationInfo)
            .execute()
    }

    override fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>? =
        handle.createQuery(
            """
                select id, username, email, password_validation, token_validation, created_at, last_used_at
                from dbo.Users as users 
                inner join dbo.Tokens as tokens 
                on users.id = tokens.user_id
                where token_validation = :validation_information
            """
        )
            .bind("validation_information", tokenValidationInfo.validationInfo)
            .mapTo<UserAndTokenModel>()
            .singleOrNull()
            ?.userAndToken

    override fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int {
        return handle.createUpdate(
            """
                delete from dbo.Tokens
                where token_validation = :validation_information
            """
        )
            .bind("validation_information", tokenValidationInfo.validationInfo)
            .execute()
    }

    private data class UserAndTokenModel(
        val id: Int,
        val username: String,
        val email: String,
        val passwordValidation: PasswordValidationInfo,
        val tokenValidation: TokenValidationInfo,
        val createdAt: Long,
        val lastUsedAt: Long
    ) {
        val userAndToken: Pair<User, Token>
            get() = Pair(
                User(id, username, email, passwordValidation),
                Token(
                    tokenValidation,
                    id,
                    Instant.fromEpochSeconds(createdAt),
                    Instant.fromEpochSeconds(lastUsedAt)
                )
            )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JdbiUsersRepository::class.java)
    }
}
