package pt.isel.gomoku.domain.games

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Represents a position in board.
 */
class Intersection(val row: Int, val column: Int) {

    @get:JsonIgnore
    val backslash: Int
        get() = -column + row

    @get:JsonIgnore
    val slash: Int
        get() = column + row

    override fun equals(other: Any?): Boolean {
        if (other !is Intersection) return false
        return this.row == other.row && this.column == other.column
    }

    override fun hashCode(): Int {
        var result = row
        result = 31 * result + column
        return result
    }
}
