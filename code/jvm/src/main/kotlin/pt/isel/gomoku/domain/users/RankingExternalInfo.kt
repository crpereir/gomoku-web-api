package pt.isel.gomoku.domain.users

data class RankingExternalInfo(
    val totalUsers: Int,
    val ranking: Ranking,
    val page: Page
)

sealed class Page {
    object Single : Page()
    class First(val next: Int, val last: Int) : Page()
    class Last(val previous: Int, val first: Int) : Page()
    class Middle(val previous: Int, val next: Int, val first: Int, val last: Int) : Page()
}
