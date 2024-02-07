package pt.isel.gomoku.services

import kotlinx.datetime.Clock
import org.springframework.stereotype.Component
import pt.isel.gomoku.domain.games.DeleteMonitorResult
import pt.isel.gomoku.domain.games.GameError.GamePlayError.TimeOut
import pt.isel.gomoku.domain.games.GameError.GetGameError.GameNotFound
import pt.isel.gomoku.domain.games.GameError.GetGameError.PlayerNotPartOfGame
import pt.isel.gomoku.domain.games.GameExternalInfo
import pt.isel.gomoku.domain.games.GameResult
import pt.isel.gomoku.domain.games.GamesDomain
import pt.isel.gomoku.domain.games.GetGameResult
import pt.isel.gomoku.domain.games.GiveUpError
import pt.isel.gomoku.domain.games.GiveUpResult
import pt.isel.gomoku.domain.games.MonitorNotFound
import pt.isel.gomoku.domain.games.StartResult
import pt.isel.gomoku.domain.games.StartResultError
import pt.isel.gomoku.domain.games.StatusMonitor
import pt.isel.gomoku.domain.games.StatusMonitorResult
import pt.isel.gomoku.domain.games.VariantError.VariantInvalid
import pt.isel.gomoku.domain.games.VariantResult
import pt.isel.gomoku.domain.games.Variants
import pt.isel.gomoku.repository.TransactionManager
import pt.isel.gomoku.utils.Failure
import pt.isel.gomoku.utils.Success
import pt.isel.gomoku.utils.failure
import pt.isel.gomoku.utils.success
import java.util.UUID

/**
 * Represents different operations related to games.
 */
@Component
class GamesService(
    private val transactionManager: TransactionManager,
    private val gamesDomain: GamesDomain,
    private val clock: Clock
) {

    /**
     * Obtains all the known game variants.
     * NOTE: Pagination are not used due to a small amount of known variations. These should be used
     * if the number of variations increases.
     */
    fun variants(): Variants {
        return transactionManager.run {
            val gamesRepository = it.gamesRepository
            gamesRepository.getVariants()
        }
    }

    /**
     * Obtains the game variant with given ID.
     */
    fun variant(variantId: Int): VariantResult {
        return transactionManager.run {
            val gamesRepository = it.gamesRepository
            if (gamesRepository.isVariantValid(variantId)) {
                success(gamesRepository.getVariant(variantId))
            } else {
                failure(VariantInvalid)
            }
        }
    }

    /**
     * Attempts to start a new game.
     */
    fun start(userId: Int, variantId: Int): StartResult {
        return transactionManager.run {
            val gamesRepository = it.gamesRepository

            // If user is waiting, then it cannot wait for another game.
            if (gamesRepository.isUserWaiting(userId)) {
                return@run failure(StartResultError.PlayerAlreadyWaiting)
            }
            // User cannot request a game with invalid variant ID.
            if (!gamesRepository.isVariantValid(variantId)) {
                return@run failure(StartResultError.VariantInvalid)
            }

            success(
                gamesRepository.start(
                    userId,
                    variantId,
                    clock.now(),
                    gamesDomain.waitingTimeInWaitingLobby,
                    gamesDomain.playingRoundTime
                )
            )
        }
    }

    /**
     * Obtains the status of game creation request.
     */
    fun statusMonitor(userId: Int, gameId: UUID): StatusMonitorResult {
        return transactionManager.run {
            val gamesRepository = it.gamesRepository

            // If there is an entry with user's game creation request, then
            // return saying it is still waiting.
            if (gamesRepository.isUserWaiting(userId, gameId)) {
                success(StatusMonitor.StillWaiting)
            } else {
                // Game could be made here.
                if (gamesRepository.isGamePresent(userId, gameId)) {
                    // Game requested has started.
                    success(StatusMonitor.Started(gameId))
                } else {
                    // Game requested did not exist. -> User waiting for invalid game.
                    failure(MonitorNotFound)
                }
            }
        }
    }

    /**
     * Giving up on waiting for a game to be created.
     */
    fun deleteMonitor(userId: Int, gameId: UUID): DeleteMonitorResult {
        return transactionManager.run {
            val gamesRepository = it.gamesRepository

            if (gamesRepository.isUserWaiting(userId, gameId)) {
                gamesRepository.deleteStatusMonitor(userId, gameId)
                success(Unit)
            } else {
                // User wasn't waiting for game in the first place.
                failure(MonitorNotFound)
            }
        }
    }

    /**
     * Obtain the information of game with given ID.
     */
    fun getById(userId: Int, gameId: UUID): GetGameResult {
        return transactionManager.run {
            val gamesRepository = it.gamesRepository
            // User cannot request games that don't exist.
            val game = gamesRepository.getById(gameId) ?: return@run failure(GameNotFound)
            // User cannot request games that does not play in.
            return@run if (!gamesDomain.userIsPlayerOfGame(userId, game)) {
                failure(PlayerNotPartOfGame)
            } else {
                success(
                    GameExternalInfo(
                        gameState = game.state,
                        playerB = game.createdPlayer,
                        playerW = game.joinedPlayer,
                        playingRoundTime = gamesDomain.playingRoundTime,
                        pieces = game.board.pieces,
                        variantId = game.variantId
                    )
                )
            }
        }
    }

    /**
     * Requesting to play a round.
     */
    fun play(userId: Int, gameId: UUID, row: Int, column: Int): GameResult {
        return transactionManager.run {
            val gamesRepository = it.gamesRepository

            // User cannot request games that don't exist.
            val game = gamesRepository.getById(gameId) ?: return@run failure(GameNotFound)
            // User cannot request games that does not play in.
            if (!gamesDomain.userIsPlayerOfGame(userId, game)) return@run failure(PlayerNotPartOfGame)

            // Obtaining the instant on when play is made.
            val now = clock.now()

            // Obtaining the information of the variant.
            val variant = gamesRepository.getVariant(game.variantId)

            when (val result = game.play(userId, row, column, variant, now)) {
                is Failure -> {
                    // If player took too long in making play.
                    if (result.value is TimeOut) {
                        gamesRepository.updateGame(gameId, game.board, game.getForfeitWinner(), game.deadline, gamesDomain.playingRoundTime)
                    }
                    return@run failure(result.value)
                }
                is Success -> {
                    val newGame = result.value
                    gamesRepository.updateGame(gameId, newGame.board, newGame.state, now, gamesDomain.playingRoundTime)
                    return@run success(
                        GameExternalInfo(
                            gameState = newGame.state,
                            playerB = newGame.createdPlayer,
                            playerW = newGame.joinedPlayer,
                            playingRoundTime = gamesDomain.playingRoundTime,
                            pieces = newGame.board.pieces,
                            variantId = newGame.variantId
                        )
                    )
                }
            }
        }
    }

    /**
     * Player giving up on game.
     */
    fun giveUp(userId: Int, gameId: UUID): GiveUpResult {
        return transactionManager.run {
            val gamesRepository = it.gamesRepository
            // User cannot request games that don't exist.
            val game = gamesRepository.getById(gameId) ?: return@run failure(GiveUpError.GameNotFound)
            // User cannot request games that does not play in.
            if (!gamesDomain.userIsPlayerOfGame(userId, game)) {
                return@run failure(GiveUpError.PlayerNotPartOfGame)
            } else {
                // User cannot give up on game that has already concluded.
                if (gamesDomain.gameIsOver(game)) {
                    return@run failure(GiveUpError.GameAlreadyEnded)
                }
                gamesRepository.updateGame(gameId, game.board, game.getForfeitWinner(), clock.now(), gamesDomain.playingRoundTime)
                return@run success(Unit)
            }
        }
    }
}
