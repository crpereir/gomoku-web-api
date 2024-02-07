package pt.isel.gomoku.domain.games

/**
 * Used to obtain all variants.
 */
data class VariantInfo(
    val id: Int,
    val boardDim: Int,
    val openingRules: OpeningRules,
    val playingRules: PlayingRules
)

typealias Variants = List<VariantInfo>
