package com.orbit.core.ui

/**
 * Bit-matrix tracking which cells in a grid page are occupied.
 * Used for collision detection when placing items on the workspace.
 */
class GridOccupancy(val columns: Int, val rows: Int) {

    private val cells = BooleanArray(columns * rows)

    fun isOccupied(x: Int, y: Int): Boolean {
        if (x < 0 || x >= columns || y < 0 || y >= rows) return true
        return cells[y * columns + x]
    }

    fun markCells(x: Int, y: Int, spanX: Int, spanY: Int, occupied: Boolean) {
        for (dy in 0 until spanY) {
            for (dx in 0 until spanX) {
                val cx = x + dx
                val cy = y + dy
                if (cx in 0 until columns && cy in 0 until rows) {
                    cells[cy * columns + cx] = occupied
                }
            }
        }
    }

    fun canPlace(x: Int, y: Int, spanX: Int, spanY: Int): Boolean {
        for (dy in 0 until spanY) {
            for (dx in 0 until spanX) {
                if (isOccupied(x + dx, y + dy)) return false
            }
        }
        return true
    }

    /**
     * Finds the nearest free position that can fit the given span, or null.
     */
    fun findNearestFree(fromX: Int, fromY: Int, spanX: Int, spanY: Int): Pair<Int, Int>? {
        var bestDist = Int.MAX_VALUE
        var best: Pair<Int, Int>? = null
        for (y in 0..rows - spanY) {
            for (x in 0..columns - spanX) {
                if (canPlace(x, y, spanX, spanY)) {
                    val dist = (x - fromX) * (x - fromX) + (y - fromY) * (y - fromY)
                    if (dist < bestDist) {
                        bestDist = dist
                        best = x to y
                    }
                }
            }
        }
        return best
    }

    fun clear() {
        cells.fill(false)
    }
}
