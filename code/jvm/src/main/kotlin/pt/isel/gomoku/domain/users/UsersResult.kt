package pt.isel.gomoku.domain.users

import pt.isel.gomoku.utils.Either

typealias UserCreationResult = Either<UserCreationError, Int>
typealias UserResult = Either<UserNotFound, User>
typealias UserStatisticsResult = Either<UserNotFound, UserStatistics>
