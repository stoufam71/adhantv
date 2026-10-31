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
 * Khatam lattice: two squares with the same circumradius, one rotated 45°, tiled across the
 * surface — the eight-point star Afnon's pattern asset (#d8b46a12) draws at full opacity.
 * Callers turn it down to a whisper; see Screensaver and AdhanCard for the alphas in use.
 */
@Composable
fun IslamicPattern(modifier: Modifier = Modifier, tile: Dp = 96.dp, color: Color = ArchLine) {
  Canvas(modifier) {
    val t = tile.toPx()
    if (t < 8f) return@Canvas
    val s = t * 0.3f // rotated square's circumradius; the axis square's corners sit on the same circle
    val d = s / sqrt(2f)
    val w = 1.dp.toPx()
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
    }
  }
}
