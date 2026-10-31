package id.adzantv.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Eight-fold khatam rosette strapwork, the pattern on Persian and Mamluk doors and ceilings: Hankin's
 * "polygons in contact" method at 67.5° on the octagon-and-square (4.8.8) tiling, drawn as a double-line
 * strap. [fade], a point in fractions of the canvas, quiets the pattern around it so text there stays clean.
 * Alternatives that were weighed: docs/design/ornament-sketch.html.
 */
@Composable
fun IslamicPattern(
  modifier: Modifier = Modifier,
  period: Dp = 220.dp,
  color: Color = PatternLine,
  fade: Offset? = null,
) {
  Spacer(
    modifier
      // offscreen so the strap's hollow and the fade cut only the pattern, never what is behind it
      .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
      .drawWithCache {
        val p = period.toPx()
        val path = Path()
        if (p >= 16f) {
          rosetteSegments(size.width, size.height, p).forEach { s ->
            path.moveTo(s[0], s[1])
            path.lineTo(s[2], s[3])
          }
        }
        val strap = Stroke(width = 8.dp.toPx(), join = StrokeJoin.Round)
        val hollow = Stroke(width = 4.dp.toPx(), join = StrokeJoin.Round) // leaves two 2dp lines
        onDrawBehind {
          drawPath(path, color, style = strap)
          drawPath(path, Color.Black, style = hollow, blendMode = BlendMode.Clear)
          if (fade != null) {
            val c = Offset(size.width * fade.x, size.height * fade.y)
            // an ellipse with the canvas's proportions: 25% strength at the point, full at the edges
            scale(1f, size.height / size.width, c) {
              drawCircle(
                Brush.radialGradient(0f to Color.Black.copy(alpha = 0.25f), 1f to Color.Black, center = c, radius = size.width * 0.45f),
                radius = size.width * 2,
                center = c,
                blendMode = BlendMode.DstIn,
              )
            }
          }
        }
      },
  )
}

/** Line segments [x1, y1, x2, y2] covering a [w]×[h] canvas, one 4.8.8 cell every [p]. */
internal fun rosetteSegments(w: Float, h: Float, p: Float, theta: Double = 67.5 * PI / 180): List<FloatArray> {
  val a = p / (1 + sqrt(2.0)) // the shared edge length of octagon and square
  val r = a / (2 * sin(PI / 8)) // octagon circumradius
  val out = ArrayList<FloatArray>()
  var x = -p.toDouble()
  while (x <= w + p) {
    var y = -p.toDouble()
    while (y <= h + p) {
      hankin(polygon(x, y, 8, r, PI / 8), theta, out)
      hankin(polygon(x + p / 2, y + p / 2, 4, a / sqrt(2.0), 0.0), theta, out)
      y += p
    }
    x += p
  }
  return out
}

private fun polygon(cx: Double, cy: Double, n: Int, r: Double, rot: Double) =
  Array(n) { i -> (rot + i * 2 * PI / n).let { doubleArrayOf(cx + r * cos(it), cy + r * sin(it)) } }

/** From each edge midpoint two rays at [theta] to the edge, leaning towards each vertex; neighbours meet. */
private fun hankin(v: Array<DoubleArray>, theta: Double, out: MutableList<FloatArray>) {
  val n = v.size
  val mids = Array(n) { i -> doubleArrayOf((v[i][0] + v[(i + 1) % n][0]) / 2, (v[i][1] + v[(i + 1) % n][1]) / 2) }
  for (i in 0 until n) {
    // the ray from edge i towards vertex i+1 meets the ray from edge i+1 towards the same vertex
    // on the line from that vertex through the centre, by symmetry; solve along that line
    val vx = v[(i + 1) % n][0]
    val vy = v[(i + 1) % n][1]
    val m = mids[i]
    val ex = vx - m[0]
    val ey = vy - m[1]
    val half = sqrt(ex * ex + ey * ey) // half the edge
    val bis = PI / 2 - PI / n // angle between the edge and the vertex-to-centre line
    val gamma = PI - theta - bis // triangle midpoint, vertex, crossing point
    if (gamma <= 0) continue
    val reach = half * sin(theta) / sin(gamma) // vertex to crossing point, law of sines
    val cx = v.sumOf { it[0] } / n
    val cy = v.sumOf { it[1] } / n
    val dx = cx - vx
    val dy = cy - vy
    val len = sqrt(dx * dx + dy * dy)
    val px = vx + dx / len * reach
    val py = vy + dy / len * reach
    val next = mids[(i + 1) % n]
    out += floatArrayOf(m[0].toFloat(), m[1].toFloat(), px.toFloat(), py.toFloat())
    out += floatArrayOf(px.toFloat(), py.toFloat(), next[0].toFloat(), next[1].toFloat())
  }
}
