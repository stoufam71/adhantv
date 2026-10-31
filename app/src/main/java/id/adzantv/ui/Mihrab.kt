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
 * Afnon's home-screen mihrab: a filled ogee arch with a gold outline and a second, inset line,
 * drawn on its 600×900 design grid and scaled to the canvas. The sides stay open at the bottom.
 * [floor] cuts the arch at that grid height, so a whole niche fits on screen; size the canvas at
 * width:height = 600:[floor] to keep the shape true.
 */
@Composable
fun Mihrab(
  modifier: Modifier = Modifier,
  line: Color = GoldLine,
  stroke: Dp = 3.dp,
  fill: Color = Surface,
  inner: Color = ArchInner,
  floor: Float = 900f,
) {
  Canvas(modifier) {
    val sx = size.width / 600f
    val sy = size.height / floor
    fun arch(inset: Float, top: Float) = Path().apply {
      moveTo(inset * sx, floor * sy)
      lineTo(inset * sx, (330 + inset / 4.5f) * sy)
      cubicTo(inset * sx, (170 + inset * 0.8f) * sy, (140 + inset * 0.6f) * sx, (70 + inset) * sy, 300 * sx, top * sy)
      cubicTo((460 - inset * 0.6f) * sx, (70 + inset) * sy, (600 - inset) * sx, (170 + inset * 0.8f) * sy, (600 - inset) * sx, (330 + inset / 4.5f) * sy)
      lineTo((600 - inset) * sx, floor * sy)
    }
    val outer = arch(0f, 0f)
    drawPath(Path().apply { addPath(outer); close() }, fill)
    drawPath(outer, line, style = Stroke(width = stroke.toPx()))
    drawPath(arch(18f, 22f), inner, style = Stroke(width = 1.5.dp.toPx()))
  }
}
