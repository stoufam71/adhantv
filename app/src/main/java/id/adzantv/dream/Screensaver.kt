package id.adzantv.dream

import android.icu.text.SimpleDateFormat
import android.icu.util.ULocale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.adzantv.time.Prayer
import id.adzantv.time.PrayerClock
import id.adzantv.time.PrayerTimes
import id.adzantv.ui.ArchLine
import id.adzantv.ui.Display
import id.adzantv.ui.Gold
import id.adzantv.ui.IslamicPattern
import id.adzantv.ui.Mihrab
import id.adzantv.ui.ScreensaverDim
import id.adzantv.ui.ScreensaverNext
import id.adzantv.ui.ScreensaverText
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.sin

data class ScreensaverData(val city: String?, val today: PrayerTimes, val tomorrow: PrayerTimes)

private val ID = Locale.forLanguageTag("id-ID")
private val TIME = DateTimeFormatter.ofPattern("HH:mm")
private val ARCH_W = 400.dp
private val ARCH_H = 580.dp // 440:size grid of Mihrab:  400 * 640 / 440 keeps the arc true
private val NUM = TextStyle(fontFeatureSettings = "tnum") // digits keep their width, so the clock does not jitter

@Composable
fun Screensaver(data: ScreensaverData?) {
  var now by remember { mutableStateOf(ZonedDateTime.now()) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(1000)
      now = ZonedDateTime.now()
    }
  }
  // slow drift against burn-in: a new spot every minute, small enough to keep the arch on screen
  val minute = now.toEpochSecond() / 60
  val dx = (sin(minute * 1.3) * 40).dp
  val dy = (sin(minute * 0.7) * 16).dp
  Box(Modifier.fillMaxSize().background(Color.Black)) {
    IslamicPattern(Modifier.fillMaxSize(), color = Gold.copy(alpha = 0.07f))
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      // keep the designed proportions on any TV: everything scales with the smaller dimension.
      // 1280x720 baseline so the composition fills a real TV instead of drifting small — 1.5x on 1080p, 3x on 4K.
      val scale = minOf(maxWidth / 1280.dp, maxHeight / 720.dp)
      val outer = LocalDensity.current
      CompositionLocalProvider(
        LocalDensity provides Density(outer.density * scale, outer.fontScale),
      ) {
        Column(Modifier.offset(dx, dy), horizontalAlignment = Alignment.CenterHorizontally) {
          Box {
            Mihrab(Modifier.width(ARCH_W).height(ARCH_H), color = ArchLine)
            Column(
              Modifier.width(ARCH_W).padding(start = 40.dp, end = 40.dp, top = 150.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              if (data?.city != null) {
                Text(data.city, color = ScreensaverDim, fontSize = 14.sp, textAlign = TextAlign.Center)
              }
              Text(now.format(TIME), color = ScreensaverText, fontSize = 88.sp, fontFamily = Display, style = NUM)
              Text(now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", ID)), color = ScreensaverDim, fontSize = 20.sp)
              Text(hijri(), color = ScreensaverDim, fontSize = 14.sp, textAlign = TextAlign.Center)
              Box(Modifier.padding(vertical = 20.dp).width(56.dp).height(1.dp).background(ArchLine))
              when {
                data == null -> {}
                data.city == null -> Text(
                  "Atur kota di aplikasi Adzan TV",
                  color = Gold,
                  fontSize = 18.sp,
                  textAlign = TextAlign.Center,
                )
                else -> Next(data, now)
              }
            }
          }
          if (data != null && data.city != null) {
            Spacer(Modifier.height(28.dp))
            Times(data, now)
          }
        }
      }
    }
  }
}

/** Next prayer and how long is left, the two things the screen exists for. */
@Composable
private fun Next(data: ScreensaverData, now: ZonedDateTime) {
  val next = PrayerClock.next(data.today, data.tomorrow, now)
  val left = Duration.between(now, next.at).seconds // toMinutesPart() needs API 31; minSdk is 28
  Text("${next.prayer.label} ${next.at.format(TIME)}", color = ScreensaverNext, fontSize = 22.sp, style = NUM)
  Text(
    "dalam %d:%02d:%02d".format(left / 3600, left % 3600 / 60, left % 60),
    color = ScreensaverDim,
    fontSize = 18.sp,
    style = NUM,
  )
}

@Composable
private fun Times(data: ScreensaverData, now: ZonedDateTime) {
  // the stored day can go stale past midnight; tomorrow then becomes today
  val today = if (data.today.date < now.toLocalDate()) data.tomorrow else data.today
  val next = PrayerClock.next(data.today, data.tomorrow, now)
  Row(horizontalArrangement = Arrangement.spacedBy(44.dp)) {
    Prayer.entries.forEach { p ->
      val isNext = p == next.prayer && next.at.toLocalDate() == today.date
      // a bar, not only gold text: marks the next prayer without relying on colour alone
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
          Modifier
            .padding(bottom = 4.dp)
            .width(30.dp)
            .height(2.dp)
            .background(if (isNext) ScreensaverNext else Color.Transparent),
        )
        Text(p.label, color = if (isNext) ScreensaverNext else ScreensaverDim, fontSize = 16.sp)
        Text(
          today.times[p]?.format(TIME) ?: "--:--",
          color = if (isNext) ScreensaverNext else ScreensaverText,
          fontSize = 22.sp,
          style = NUM,
        )
      }
    }
  }
  if (today.estimated) {
    Spacer(Modifier.height(20.dp))
    Text("perkiraan — jadwal Kemenag belum tersedia", color = ScreensaverDim, fontSize = 16.sp)
  }
}

/** Tabular (civil) Hijri date; may differ by a day from Indonesian moon sighting. */
private fun hijri(): String {
  // the calendar keyword makes ICU use Islamic month names; a plain id_ID locale prints Gregorian ones
  val fmt = SimpleDateFormat("d MMMM y 'H'", ULocale("id_ID@calendar=islamic-civil"))
  return fmt.format(Date())
}
