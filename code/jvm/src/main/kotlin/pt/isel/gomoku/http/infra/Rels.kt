package pt.isel.gomoku.http.infra

object Rels {

    private const val DOC_LOCATION = "https://github.com/isel-leic-daw/2023-daw-leic51d-07/tree/main/docs/api/rel/"

    val SELF = LinkRelation("self")

    val PREVIOUS = LinkRelation("previous")

    val NEXT = LinkRelation("next")

    val FIRST = LinkRelation("first")

    val LAST = LinkRelation("last")

    val USER_HOME = LinkRelation(DOC_LOCATION + "userHome")

    val ABOUT = LinkRelation(DOC_LOCATION + "about")

    val RANKING = LinkRelation(DOC_LOCATION + "ranking")

    val STATISTICS = LinkRelation(DOC_LOCATION + "statistics")

    val VARIANTS = LinkRelation(DOC_LOCATION + "variants")

    val VARIANT = LinkRelation(DOC_LOCATION + "variant")

    val GAME = LinkRelation(DOC_LOCATION + "game")

    val MONITOR = LinkRelation(DOC_LOCATION + "monitor")
}
