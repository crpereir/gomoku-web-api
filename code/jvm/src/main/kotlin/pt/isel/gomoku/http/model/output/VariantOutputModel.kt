package pt.isel.gomoku.http.model.output

data class VariantOutputModel(
    val variants: List<VariantInfo>
)

data class VariantInfo(
    val variantId: Int,
    val boardDim: Int,
    val openingRules: String,
    val playingRules: String
)
