package pt.isel.gomoku.domain.system

/**
 * Represents a version in a
 * [Semantic-versioning](https://en.wikipedia.org/wiki/Software_versioning#Semantic_versioning) scheme.
 */
class Version(
    private val major: Int,
    private val minor: Int,
    private val patch: Int
) {
    override fun toString() = "$major.$minor.$patch"
}
