package id.adzantv.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class IslamicPatternTest {
  /** The law-of-sines shortcut must land where the two Hankin rays really cross. */
  @Test
  fun octagonCrossingsMatchRayIntersection() {
    val p = 220f
    val theta = 67.5 * PI / 180
    val segs = rosetteSegments(100f, 100f, p, theta)
    // the first cell is the octagon centred at (-p, -p): 8 edges, two segments each
    val a = p / (1 + sqrt(2.0))
    val r = a / (2 * sin(PI / 8))
    val v = Array(8) { i -> (PI / 8 + i * PI / 4).let { doubleArrayOf(-p + r * cos(it), -p + r * sin(it)) } }
    for (i in 0 until 8) {
      val expected = cross(v[i], v[(i + 1) % 8], v[(i + 2) % 8], theta, centre = doubleArrayOf(-p.toDouble(), -p.toDouble()))
      val x = segs[2 * i]
      assertEquals(expected[0], x[2].toDouble(), 1e-2)
      assertEquals(expected[1], x[3].toDouble(), 1e-2)
    }
  }

  /** Ray from the midpoint of a-b towards b, and from the midpoint of b-c towards b, both at theta, bent inwards. */
  private fun cross(a: DoubleArray, b: DoubleArray, c: DoubleArray, theta: Double, centre: DoubleArray): DoubleArray {
    fun ray(from: DoubleArray, to: DoubleArray, towards: DoubleArray): Pair<DoubleArray, DoubleArray> {
      val m = doubleArrayOf((from[0] + to[0]) / 2, (from[1] + to[1]) / 2)
      val l = sqrt((towards[0] - m[0]).let { it * it } + (towards[1] - m[1]).let { it * it })
      val d = doubleArrayOf((towards[0] - m[0]) / l, (towards[1] - m[1]) / l)
      var n = doubleArrayOf(-d[1], d[0])
      if ((centre[0] - m[0]) * n[0] + (centre[1] - m[1]) * n[1] < 0) n = doubleArrayOf(-n[0], -n[1])
      return m to doubleArrayOf(d[0] * cos(theta) + n[0] * sin(theta), d[1] * cos(theta) + n[1] * sin(theta))
    }
    val (p, d) = ray(a, b, b)
    val (q, e) = ray(b, c, b)
    val den = d[0] * e[1] - d[1] * e[0]
    val t = ((q[0] - p[0]) * e[1] - (q[1] - p[1]) * e[0]) / den
    return doubleArrayOf(p[0] + d[0] * t, p[1] + d[1] * t)
  }
}
