package id.adzantv.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Afnon's mihrab outline: open at the bottom, drawn on its 440×640 design grid and scaled to the canvas.
 * Size the canvas with the modifier.
 */
@Composable
fun Mihrab(modifier: Modifier = Modifier, color: Color = ArchLine, stroke: Dp = 1.dp) {
  Canvas(modifier) {
    val sx = size.width / 440f
    val sy = size.height / 640f
    val path = Path().apply {
      moveTo(2 * sx, 580 * sy)
      lineTo(2 * sx, 230 * sy)
      cubicTo(2 * sx, 120 * sy, 100 * sx, 52 * sy, 220 * sx, 2 * sy)
      cubicTo(340 * sx, 52 * sy, 438 * sx, 120 * sy, 438 * sx, 230 * sy)
      lineTo(438 * sx, 580 * sy)
    }
    drawPath(path, color, style = Stroke(width = stroke.toPx()))
  }
}
