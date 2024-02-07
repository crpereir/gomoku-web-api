package pt.isel.gomoku.repository.jdbi

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.datetime.Instant
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.statement.Update
import org.postgresql.util.PGobject
import pt.isel.gomoku.domain.games.Board
import pt.isel.gomoku.domain.games.Game
import pt.isel.gomoku.domain.games.Variant
import pt.isel.gomoku.domain.games.VariantInfo
import pt.isel.gomoku.domain.games.Variants
import pt.isel.gomoku.domain.games.WaitingLobbyRegister
import pt.isel.gomoku.repository.GamesRepository
import java.util.UUID
import kotlin.time.Duration

/**
 * Represents the status of game after start attempt.
 */
sealed class StartStatus {
    data class Create(val id: UUID) : StartStatus()
    data class Register(val id: UUID) : StartStatus()
}

/**
 * Represents database access related to game operations using JDBI.
 */
class JdbiGamesRepository(
    private val handle: Handle
) : GamesRepository {

    override fun isUserWaiting(userId: Int): Boolean =
        handle.createQuery("select id from dbo.Waiting_Lobby where created_user_id = :created_user_id")
            .bind("created_user_id", userId)
            .mapTo<UUID>()
            .singleOrNull() != null

    override fun isUserWaiting(userId: Int, gameId: UUID): Boolean {
        return handle.createQuery(
            """
                select id from dbo.Waiting_Lobby
                where created_user_id = :created_user_id and game_id = :game_id 
            """
        )
            .bind("created_user_id", userId)
            .bind("game_id", gameId)
            .mapTo<UUID>()
            .singleOrNull() != null
    }

    override fun start(
        userId: Int,
        variantId: Int,
        currentTime: Instant,
        waitingTimeInWaitingLobby: Duration,
        playingRoundTime: Duration
    ): StartStatus {
        val waitingLobbyRegister = handle.createQuery(
            """
                select id, created_user_id, created_at, deadline, variant_id, game_id
                from dbo.Waiting_Lobby
                where variant_id = :variant_id
                for update
            """
        )
            .bind("variant_id", variantId)
            .mapTo<WaitingLobbyRegister>()
            .firstOrNull()

        return if (waitingLobbyRegister == null) {
            registerInWaitingLobby(userId, currentTime, variantId, waitingTimeInWaitingLobby)
        } else {
            createGame(waitingLobbyRegister, userId, currentTime, playingRoundTime)
        }
    }

    /**
     * Registers the game creation request, allowing for other users to join.
     */
    private fun registerInWaitingLobby(
        userId: Int,
        currentTime: Instant,
        variantId: Int,
        waitingTimeInWaitingLobby: Duration
    ): StartStatus {
        val monitorId = UUID.randomUUID()
        val gameId = UUID.randomUUID()
        val insertResult = handle.createUpdate(
            """
               insert into dbo.Waiting_Lobby (id, created_user_id, created_at, deadline, variant_id, game_id) values 
               (:id, :created_user_id, :created_at, :deadline, :variant_id, :game_id)
            """
        )
            .bind("id", monitorId)
            .bind("created_user_id", userId)
            .bind("created_at", currentTime.epochSeconds)
            .bind("deadline", currentTime.plus(waitingTimeInWaitingLobby).epochSeconds)
            .bind("variant_id", variantId)
            .bind("game_id", gameId)
            .execute()
        return if (insertResult == 1) {
            StartStatus.Register(gameId)
        } else {
            val newGameId = handle.createQuery(
                """
                        select g.id from dbo.Games g where g.joined_player = :user_id and 
                        (g.state like 'NEXT_TURN_B' or g.state like 'NEXT_TURN_W')
                """.trimIndent()
            )
                .bind("user_id", userId)
                .mapTo<UUID>()
                .first()
            StartStatus.Create(newGameId)
        }
    }

    /**
     * Creates a game.
     */
    private fun createGame(
        waitingLobbyRegister: WaitingLobbyRegister,
        userId: Int,
        currentTime: Instant,
        playingRoundTime: Duration
    ): StartStatus.Create {
        deleteStatusMonitor(waitingLobbyRegister.createdUserId, waitingLobbyRegister.gameId)

        handle.createUpdate(
            """
               insert into dbo.Games (id, board, state, created_at, updated_at, deadline, created_player, joined_player, variant_id) values 
               (:id, :board, :state, :created_at, :updated_at, :deadline, :created_player, :joined_player, :variant_id)
            """
        )
            .bind("id", waitingLobbyRegister.gameId)
            .bindBoard("board", Board.initial)
            .bind("state", Game.State.NEXT_TURN_B)
            .bind("created_at", currentTime.epochSeconds)
            .bind("updated_at", currentTime.epochSeconds)
            .bind("deadline", currentTime.plus(playingRoundTime).epochSeconds)
            .bind("created_player", waitingLobbyRegister.createdUserId)
            .bind("joined_player", userId)
            .bind("variant_id", waitingLobbyRegister.variantId)
            .execute()
        return StartStatus.Create(waitingLobbyRegister.gameId)
    }

    override fun deleteStatusMonitor(userId: Int, gameId: UUID) {
        handle.createUpdate("delete from dbo.Waiting_Lobby where game_id = :game_id and created_user_id = :created_user_id")
            .bind("game_id", gameId)
            .bind("created_user_id", userId)
            .execute()
    }

    override fun getById(gameId: UUID): Game? =
        handle.createQuery(
            """
                select id, board, state, created_at, updated_at, deadline, created_player, joined_player, variant_id
                from dbo.Games 
                where id = :id
            """.trimIndent()
        ).bind("id", gameId)
            .mapTo<Game>()
            .singleOrNull()

    override fun isGamePresent(userId: Int, gameId: UUID): Boolean =
        handle.createQuery("select id from dbo.Games where id = :game_id and created_player = :created_player")
            .bind("game_id", gameId)
            .bind("created_player", userId)
            .mapTo<UUID>()
            .singleOrNull() != null

    override fun isVariantValid(variantId: Int): Boolean =
        handle.createQuery("select count(*) from dbo.Variants where id = :id")
            .bind("id", variantId)
            .mapTo<Int>()
            .single() == 1

    override fun getVariants(): Variants =
        handle.createQuery("select id, board_dim, opening_rules, playing_rules from dbo.Variants")
            .mapTo<VariantInfo>()
            .list()

    override fun getVariant(variantId: Int): Variant =
        handle.createQuery(
            """
                select board_dim, opening_rules, playing_rules
                from dbo.Variants
                where id = :id
            """.trimIndent()
        )
            .bind("id", variantId)
            .mapTo<Variant>()
            .one()

    override fun updateGame(
        gameId: UUID,
        newBoard: Board,
        newState: Game.State,
        updateTime: Instant,
        playingRoundTime: Duration
    ) {
        handle.createUpdate(
            """
                update dbo.Games set board = :board, updated_at = :updated_at, deadline = :deadline, state = :state
                where id = :gameId
            """.trimIndent()
        )
            .bindBoard("board", newBoard)
            .bind("state", newState.toString())
            .bind("updated_at", updateTime.epochSeconds)
            .bind("deadline", updateTime.plus(playingRoundTime).epochSeconds)
            .bind("gameId", gameId)
            .execute()
    }

    companion object {
        private fun Update.bindBoard(name: String, board: Board) = run {
            bind(
                name,
                PGobject().apply {
                    type = "jsonb"
                    value = serializeBoardToJson(board)
                }
            )
        }

        private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

        private fun serializeBoardToJson(board: Board): String =
            objectMapper.writeValueAsString(board)

        fun deserializeBoardFromJson(json: String): Board =
            objectMapper.readValue(json, Board::class.java)
    }
}
