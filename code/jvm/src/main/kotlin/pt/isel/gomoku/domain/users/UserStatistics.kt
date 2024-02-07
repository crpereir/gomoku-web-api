package pt.isel.gomoku.domain.users

/**
 * Represents a user's statistics, including result in several games played.
 */
data class UserStatistics(
    val userId: Int,
    val username: String,
    val rankingPosition: Int,
    val gamesPlayed: Int,
    val gamesWon: Int,
    val gamesLost: Int,
    val gamesTied: Int
)

typealias Ranking = List<UserStatistics>
