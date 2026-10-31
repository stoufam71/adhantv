package id.adzantv.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Afnon's "star8" khatam lattice on its 96 grid: a 40×40 square and the same square rotated 45°
 * (an eight-point star), with short bars joining each star to its neighbours.
 */
@Composable
fun IslamicPattern(modifier: Modifier = Modifier, tile: Dp = 96.dp, color: Color = PatternLine) {
  Canvas(modifier) {
    val t = tile.toPx()
    if (t < 8f) return@Canvas
    val d = t * 20f / 96f // half the square's side
    val s = d * sqrt(2f) // the rotated square's corners sit on the same circle
    val bar = t * 20f / 96f
    val w = 1.5.dp.toPx()
    val cols = ceil(size.width / t).toInt() + 1
    val rows = ceil(size.height / t).toInt() + 1
    for (i in -1..cols) for (j in -1..rows) {
      val cx = (i + 0.5f) * t
      val cy = (j + 0.5f) * t
      // axis-aligned square
      drawLine(color, Offset(cx - d, cy - d), Offset(cx + d, cy - d), w)
      drawLine(color, Offset(cx + d, cy - d), Offset(cx + d, cy + d), w)
      drawLine(color, Offset(cx + d, cy + d), Offset(cx - d, cy + d), w)
      drawLine(color, Offset(cx - d, cy + d), Offset(cx - d, cy - d), w)
      // the same square rotated 45°
      drawLine(color, Offset(cx - s, cy), Offset(cx, cy - s), w)
      drawLine(color, Offset(cx, cy - s), Offset(cx + s, cy), w)
      drawLine(color, Offset(cx + s, cy), Offset(cx, cy + s), w)
      drawLine(color, Offset(cx, cy + s), Offset(cx - s, cy), w)
      // bars to the tile edges, so neighbouring stars link into one lattice
      drawLine(color, Offset(cx - t / 2, cy), Offset(cx - t / 2 + bar, cy), w)
      drawLine(color, Offset(cx + t / 2 - bar, cy), Offset(cx + t / 2, cy), w)
      drawLine(color, Offset(cx, cy - t / 2), Offset(cx, cy - t / 2 + bar), w)
      drawLine(color, Offset(cx, cy + t / 2 - bar), Offset(cx, cy + t / 2), w)
    }
  }
}
