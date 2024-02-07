package pt.isel.gomoku.domain.games

import pt.isel.gomoku.repository.jdbi.StartStatus
import pt.isel.gomoku.utils.Either

typealias VariantResult = Either<VariantError, Variant>

typealias StartResult = Either<StartResultError, StartStatus>

typealias StatusMonitorResult = Either<MonitorNotFound, StatusMonitor>
typealias DeleteMonitorResult = Either<MonitorNotFound, Unit>

typealias GameResult = Either<GameError, GameExternalInfo>

typealias GetGameResult = Either<GameError.GetGameError, GameExternalInfo>

typealias BoardPlayResult = Either<GameError.GamePlayError, Board>
typealias GamePlayResult = Either<GameError.GamePlayError, Game>

typealias GiveUpResult = Either<GiveUpError, Unit>
