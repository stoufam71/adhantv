package id.adzantv.ui

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val NUM = TextStyle(fontFeatureSettings = "tnum")

/** Arabic names under the Indonesian ones; keyed by the label the alarm passes in. */
private val ARABIC = mapOf(
  "Subuh" to "صَلَاةُ الصُّبْح",
  "Dzuhur" to "صَلَاةُ الظُّهْر",
  "Ashar" to "صَلَاةُ العَصْر",
  "Maghrib" to "صَلَاةُ المَغْرِب",
  "Isya" to "صَلَاةُ العِشَاء",
)

/**
 * Afnon's call screen: a wide, brighter-edged mihrab standing whole on the ornament, content centred in it.
 * The arch rises in once, then its edge breathes slowly while the adhan sounds, and a progress line
 * shows how much of the call is left. [progress] returns played and total milliseconds, or null.
 */
@Composable
fun AdhanCard(prayerLabel: String, at: LocalTime, city: String?, progress: () -> Pair<Int, Int>? = { null }) {
  // the system "remove animations" switch sets the scale to 0: then nothing moves
  val ctx = LocalContext.current
  val still = remember { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
  val enter = remember { Animatable(if (still) 1f else 0f) }
  LaunchedEffect(Unit) { enter.animateTo(1f, tween(700, easing = LinearOutSlowInEasing)) }
  val breath by rememberInfiniteTransition(label = "breath").animateFloat(
    initialValue = 1f,
    targetValue = if (still) 1f else 0.55f,
    animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
    label = "edge",
  )

  Stage {
    Box(
      Modifier.fillMaxSize().graphicsLayer {
        alpha = enter.value
        translationY = (1 - enter.value) * 48.dp.toPx()
      },
    ) {
      val edge = GoldLight.copy(alpha = breath)
      // a whole niche, wide for a 16:9 screen: 840 wide, cut at grid height 657 so it ends at 980
      Mihrab(Modifier.offset(540.dp, 60.dp).size(840.dp, 920.dp), line = edge, stroke = 6.dp, floor = 920f * 600f / 840f)
      // the niche stands on a double gold base line, like a prayer niche on its sill
      Box(Modifier.offset(480.dp, 978.dp).size(960.dp, 4.dp).background(edge))
      Box(Modifier.offset(510.dp, 994.dp).size(900.dp, 2.dp).background(ArchInner))
      Box(Modifier.offset(540.dp, 60.dp).size(840.dp, 920.dp).padding(top = 60.dp), contentAlignment = Alignment.Center) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("حَيَّ عَلَى الصَّلَاةِ", color = Gold, fontSize = 60.sp, lineHeight = 1.6.em, fontFamily = Arabic)
        Text("WAKTUNYA SHALAT", color = Mist, fontSize = 22.sp, letterSpacing = 0.12.em, modifier = Modifier.padding(top = 24.dp))
        Text(prayerLabel, color = Ivory, fontSize = 128.sp, lineHeight = 1.1.em, fontFamily = Display, fontWeight = FontWeight.Bold)
        ARABIC[prayerLabel]?.let { Text(it, color = Gold, fontSize = 44.sp, lineHeight = 1.6.em, fontFamily = Arabic) }
        Text(
          at.format(DateTimeFormatter.ofPattern("HH:mm")) + (city?.let { "  ·  $it" } ?: ""),
          color = Mist,
          fontSize = 28.sp,
          style = NUM,
          modifier = Modifier.padding(top = 12.dp),
        )
        Progress(progress)
        // Afnon's focused pill: shows what OK does. Any key stops the call, so it needs no real focus.
        Text(
          "Hentikan adzan",
          color = Ground,
          fontSize = 28.sp,
          fontWeight = FontWeight.Medium,
          textAlign = TextAlign.Center,
          modifier = Modifier
            .padding(top = 44.dp)
            .border(4.dp, Ivory, RoundedCornerShape(50))
            .padding(6.dp)
            .background(Gold, RoundedCornerShape(50))
            .width(420.dp)
            .padding(vertical = 18.dp),
        )
        Text("OK atau Kembali", color = Mist, fontSize = 22.sp, modifier = Modifier.padding(top = 16.dp))
      }
      }
    }
  }
}

/** A thin line filling as the adhan plays, with time played and total; hidden when there is no sound. */
@Composable
private fun Progress(progress: () -> Pair<Int, Int>?) {
  var now by remember { mutableStateOf(progress()) }
  LaunchedEffect(Unit) {
    while (true) {
      now = progress()
      delay(500)
    }
  }
  val (played, total) = now?.takeIf { it.second > 0 } ?: return
  Column(Modifier.padding(top = 36.dp).width(440.dp)) {
    Box(Modifier.fillMaxWidth().height(4.dp).background(LineSoft, RoundedCornerShape(2.dp))) {
      Box(
        Modifier
          .fillMaxHeight()
          .fillMaxWidth((played.toFloat() / total).coerceIn(0f, 1f))
          .background(Gold, RoundedCornerShape(2.dp)),
      )
    }
    Text(
      "${clock(played)} / ${clock(total)}",
      color = Mist,
      fontSize = 22.sp,
      style = NUM,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
    )
  }
}

private fun clock(ms: Int) = (ms / 1000).let { "%d:%02d".format(it / 60, it % 60) }
