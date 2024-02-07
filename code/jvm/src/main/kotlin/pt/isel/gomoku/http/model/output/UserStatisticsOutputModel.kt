package pt.isel.gomoku.http.model.output

data class UserStatisticsOutputModel(
    val totalUsers: Int,
    val ranking: List<UserStatisticsInfo>
)

data class UserStatisticsInfo(
    val userId: Int,
    val username: String,
    val rankingPosition: Int,
    val gamesPlayed: Int,
    val gamesWon: Int,
    val gamesLost: Int,
    val gamesTied: Int
)
