package pt.isel.gomoku.domain.token

import pt.isel.gomoku.utils.Either

typealias TokenCreationResult = Either<TokenCreationError, TokenExternalInfo>
