package id.adzantv.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.adzantv.ui.Display
import id.adzantv.ui.SettingsColors as C

// The shared parts of every settings screen: large type (nothing under 26), outlined rows, fixed colours.
// Design: docs/design/screens.figcraft, frames 11 to 14.

/** Fast, so the D-pad feels direct. */
private const val FOCUS_MS = 120

/**
 * Any D-pad target. Focus is a plain light fill with dark text and a small lift; colour and scale only, so
 * neighbours never move. A disabled target dims and is skipped by the D-pad.
 */
@Composable
internal fun FocusBox(
  modifier: Modifier,
  onClick: () -> Unit,
  enabled: Boolean = true,
  radius: Dp = 18.dp,
  content: @Composable (focused: Boolean) -> Unit,
) {
  var focused by remember { mutableStateOf(false) }
  val scale by animateFloatAsState(if (focused) 1.02f else 1f, tween(FOCUS_MS), label = "focus")
  val shape = RoundedCornerShape(radius)
  Box(
    modifier
      .graphicsLayer { scaleX = scale; scaleY = scale }
      .alpha(if (enabled) 1f else 0.4f)
      .onFocusChanged { focused = it.isFocused }
      .background(if (focused) C.Focus else C.Row, shape)
      .border(2.dp, if (focused) C.Focus else C.Edge, shape)
      .clickable(enabled = enabled, onClick = onClick),
  ) {
    content(focused)
  }
}

/** One row: icon and label on the left, an optional hint under it; a value, a switch or a chevron on the right. */
@Composable
internal fun SettingRow(
  icon: String,
  label: String,
  modifier: Modifier = Modifier,
  hint: String? = null,
  value: String? = null,
  chevron: Boolean = false,
  toggle: Boolean? = null,
  enabled: Boolean = true,
  onClick: () -> Unit,
) {
  FocusBox(modifier.fillMaxWidth().height(116.dp), onClick, enabled) { focused ->
    val fg = if (focused) C.Ground else C.Text
    Row(Modifier.fillMaxSize().padding(horizontal = 32.dp), verticalAlignment = Alignment.CenterVertically) {
      Icon(icon, fg)
      Spacer(Modifier.width(32.dp))
      Column(Modifier.weight(1f)) {
        Text(label, color = fg, fontSize = 34.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        if (hint != null) Text(hint, color = if (focused) C.Ground else C.Dim, fontSize = 26.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      if (value != null) Text(value, color = fg, fontSize = 30.sp, fontWeight = FontWeight.Medium, maxLines = 1)
      if (toggle != null) Switch(toggle, focused)
      if (chevron) {
        Spacer(Modifier.width(20.dp))
        Chevron(fg)
      }
    }
  }
}

/** The state as a word and a switch, so it never rests on colour alone. */
@Composable
internal fun RowScope.Switch(on: Boolean, focused: Boolean) {
  val c = if (focused) C.Ground else if (on) C.On else C.Dim
  Text(if (on) "Nyala" else "Mati", color = c, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
  Spacer(Modifier.width(20.dp))
  Box(
    Modifier
      .size(80.dp, 44.dp)
      .then(if (on) Modifier.background(c, CircleShape) else Modifier.border(3.dp, c, CircleShape))
      .padding(horizontal = 6.dp),
    contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
  ) {
    Box(Modifier.size(if (on) 32.dp else 26.dp).background(if (!on) c else if (focused) C.Focus else C.Ground, CircleShape))
  }
}

/** A box with a check mark, or an empty outline: "chosen" as a mark, not only as green. */
@Composable
internal fun CheckBox(checked: Boolean, focused: Boolean) {
  val shape = RoundedCornerShape(10.dp)
  val fill = if (focused) C.Ground else C.On
  Box(
    Modifier.size(44.dp).then(if (checked) Modifier.background(fill, shape) else Modifier.border(3.dp, if (focused) C.Ground else C.Edge, shape)),
    contentAlignment = Alignment.Center,
  ) {
    if (checked) Icon(CHECK, if (focused) C.Focus else C.Ground, Modifier.size(32.dp), stroke = 3.2f)
  }
}

// 28×28 stroke icons, as SVG path data
internal const val PIN = "M14 3a8 8 0 0 0-8 8c0 6 8 14 8 14s8-8 8-14a8 8 0 0 0-8-8z M11 11a3 3 0 1 0 6 0a3 3 0 1 0-6 0"
internal const val TARGET = "M6 14a8 8 0 1 0 16 0a8 8 0 1 0-16 0 M14 2v6 M14 20v6 M2 14h6 M20 14h6"
internal const val BELL = "M5 21h18l-2.5-3.5v-6.5a6.5 6.5 0 0 0-13 0v6.5z M11.5 24.5a2.5 2.5 0 0 0 5 0"
internal const val ALARM = "M5 15a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M14 10v5l3.5 2 M4 7l3.5-3.5 M24 7l-3.5-3.5"
internal const val PLAY = "M9 5l14 9-14 9z"
internal const val MOON = "M23 17.5A10.5 10.5 0 1 1 10.5 5a8.5 8.5 0 0 0 12.5 12.5z"
internal const val PALETTE = "M3 14a11 11 0 1 0 22 0a11 11 0 1 0-22 0 M14 3v22 M14 8h6 M14 14h10 M14 20h6"
internal const val SEARCH = "M4 12a8 8 0 1 0 16 0a8 8 0 1 0-16 0 M18 18l7 7"
internal const val CHECK = "M5 14.5l6 6 12-13"

/** A drawn icon, not a font glyph: same weight on every TV. */
@Composable
internal fun Icon(data: String, color: Color, modifier: Modifier = Modifier.size(36.dp), stroke: Float = 2f) {
  val path = remember(data) { PathParser().parsePathString(data).toPath() }
  Canvas(modifier) {
    scale(size.width / 28f, pivot = Offset.Zero) {
      drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
  }
}

/** Points right; [left] mirrors it. */
@Composable
internal fun Chevron(color: Color, left: Boolean = false) {
  Canvas(Modifier.size(15.dp, 30.dp).graphicsLayer { scaleX = if (left) -1f else 1f }) {
    val w = 4.dp.toPx()
    drawLine(color, Offset(0f, 0f), Offset(size.width, size.height / 2), w, StrokeCap.Round)
    drawLine(color, Offset(size.width, size.height / 2), Offset(0f, size.height), w, StrokeCap.Round)
  }
}

@Composable
internal fun Title(text: String, modifier: Modifier = Modifier) {
  Text(text, color = C.Text, fontSize = 56.sp, fontFamily = Display, fontWeight = FontWeight.Bold, modifier = modifier)
}

/** What just happened, in one fixed place so the rows never move. The check mark is only for things that worked. */
@Composable
internal fun Status(message: Message, modifier: Modifier = Modifier) {
  val shape = CircleShape
  Row(
    modifier.height(64.dp).background(C.Row, shape).border(2.dp, C.Edge, shape).padding(horizontal = 28.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (message.ok) {
      Icon(CHECK, C.On, Modifier.size(34.dp), stroke = 2.4f)
      Spacer(Modifier.width(16.dp))
    }
    Text(message.text, color = C.Text, fontSize = 30.sp, maxLines = 1)
  }
}

/** A status line; [ok] false is a notice or a problem, shown without the success mark. */
internal data class Message(val text: String, val ok: Boolean = true)

/** What the remote does here, in words on key caps: (key, what it does). */
@Composable
internal fun Hints(modifier: Modifier, vararg keys: Pair<String, String>) {
  Row(modifier, verticalAlignment = Alignment.CenterVertically) {
    keys.forEach { (cap, label) ->
      Text(
        cap,
        color = C.Dim,
        fontSize = 26.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.border(2.dp, C.Dim, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 6.dp),
      )
      Text(label, color = C.Dim, fontSize = 26.sp, modifier = Modifier.padding(start = 14.dp, end = 48.dp))
    }
  }
}
