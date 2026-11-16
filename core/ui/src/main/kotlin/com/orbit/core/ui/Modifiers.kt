package com.orbit.core.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Shared modifier extensions for the Orbit launcher.
 */

/**
 * Draws a debug grid overlay when enabled.
 */
fun Modifier.debugGrid(
    enabled: Boolean = false,
    columns: Int = 4,
    rows: Int = 5,
    color: Color = Color.Red.copy(alpha = 0.15f),
): Modifier = if (!enabled) this else drawBehind {
    val cellW = size.width / columns
    val cellH = size.height / rows
    for (c in 0..columns) {
        drawLine(color, start = androidx.compose.ui.geometry.Offset(c * cellW, 0f), end = androidx.compose.ui.geometry.Offset(c * cellW, size.height))
    }
    for (r in 0..rows) {
        drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, r * cellH), end = androidx.compose.ui.geometry.Offset(size.width, r * cellH))
    }
}
