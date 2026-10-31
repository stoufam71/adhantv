package id.adzantv.dream

import android.icu.text.SimpleDateFormat
import android.icu.util.ULocale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import id.adzantv.time.Prayer
import id.adzantv.time.PrayerClock
import id.adzantv.time.PrayerTimes
import id.adzantv.ui.ArchInner
import id.adzantv.ui.Display
import id.adzantv.ui.Gold
import id.adzantv.ui.GoldLight
import id.adzantv.ui.GoldLine
import id.adzantv.ui.SurfacePast
import id.adzantv.ui.Ivory
import id.adzantv.ui.Surface
import id.adzantv.ui.Mihrab
import id.adzantv.ui.Mist
import id.adzantv.ui.Stage
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.sin

data class ScreensaverData(val city: String?, val today: PrayerTimes, val tomorrow: PrayerTimes, val adhanOn: Boolean = true)

private val ID = Locale.forLanguageTag("id-ID")
private val TIME = DateTimeFormatter.ofPattern("HH:mm")
private val NUM = TextStyle(fontFeatureSettings = "tnum") // digits keep their width, so the clock does not jitter

/** An-Nisa 4:103, the verse Afnon sets in its arch: prayer is prescribed at fixed times. */
private const val VERSE = "إِنَّ الصَّلَاةَ كَانَتْ\nعَلَى الْمُؤْمِنِينَ\nكِتَابًا مَّوْقُوتًا"

/**
 * Afnon's home composition on a 1920×1080 stage: the mihrab on the left holds the verse and the
 * next prayer's countdown; the courtyard on the right holds the clock, dates and the day's six times.
 */
@Composable
fun Screensaver(data: ScreensaverData?) {
  var now by remember { mutableStateOf(ZonedDateTime.now()) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(1000)
      now = ZonedDateTime.now()
    }
  }
  // slow drift against burn-in: a new spot every minute, inside the 96dp safe margin
  val minute = now.toEpochSecond() / 60
  // the ornament quiets behind the clock and the prayer cells
  Stage(dx = (sin(minute * 1.3) * 40).dp, dy = (sin(minute * 0.7) * 24).dp, quiet = Offset(1330f, 560f)) {
    // Afnon's "approaching" arch: a brighter, thicker edge in the last 10 minutes, readable from across the room
    val near = data?.city != null && Duration.between(now, PrayerClock.next(data.today, data.tomorrow, now).at).toMinutes() < 10
    val edge = if (near) GoldLight else GoldLine
    // a whole niche like the call screen: 680 wide, cut at grid height 768 so it ends at 960,
    // leaving room for the base lines and the drift
    Mihrab(
      Modifier.offset(96.dp, 90.dp).size(680.dp, 870.dp),
      line = edge,
      stroke = if (near) 6.dp else 3.dp,
      floor = 870f * 600f / 680f,
    )
    Box(Modifier.offset(56.dp, 958.dp).size(760.dp, 4.dp).background(edge))
    Box(Modifier.offset(80.dp, 974.dp).size(712.dp, 2.dp).background(ArchInner))
    // content centred in the arch body, nudged below the narrow crown
    Box(Modifier.offset(96.dp, 90.dp).size(680.dp, 870.dp).padding(top = 70.dp), contentAlignment = Alignment.Center) {
    Column(Modifier.width(560.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(VERSE, color = Gold, fontSize = 34.sp, lineHeight = 1.5.em, fontFamily = Display, textAlign = TextAlign.Center)
      Text("QS An-Nisa: 103", color = Mist, fontSize = 20.sp, modifier = Modifier.padding(top = 8.dp))
      Box(Modifier.padding(vertical = 28.dp).width(120.dp).height(2.dp).background(GoldLine))
      when {
        data == null -> {}
        data.city == null -> Text("Atur kota di aplikasi Adzan TV", color = Gold, fontSize = 30.sp, textAlign = TextAlign.Center)
        else -> Next(data, now)
      }
    }
    }
    Courtyard(data, now)
  }
}

/** Next prayer and how long is left, the two things the screen exists for. */
@Composable
private fun Next(data: ScreensaverData, now: ZonedDateTime) {
  val next = PrayerClock.next(data.today, data.tomorrow, now)
  val left = Duration.between(now, next.at).seconds // toMinutesPart() needs API 31; minSdk is 28
  Text("SHALAT BERIKUTNYA", color = Mist, fontSize = 22.sp, letterSpacing = 0.12.em)
  Text(next.prayer.label, color = Gold, fontSize = 88.sp, fontFamily = Display, fontWeight = FontWeight.Bold, lineHeight = 1.1.em)
  Text(
    "%d:%02d:%02d".format(left / 3600, left % 3600 / 60, left % 60),
    color = Ivory,
    fontSize = 110.sp,
    fontFamily = Display,
    lineHeight = 1.em,
    style = NUM,
  )
  Text(if (data.adhanOn) "Adzan pukul ${next.at.format(TIME)}" else "Adzan dimatikan", color = Mist, fontSize = 26.sp, modifier = Modifier.padding(top = 12.dp), style = NUM)
}

@Composable
private fun Courtyard(data: ScreensaverData?, now: ZonedDateTime) {
  Column(Modifier.offset(840.dp, 310.dp).width(984.dp)) {
    Text(now.format(TIME), color = Ivory, fontSize = 168.sp, fontFamily = Display, lineHeight = 1.em, style = NUM)
    Text(
      hijri() + (data?.city?.let { "  ·  $it" } ?: ""),
      color = Ivory,
      fontSize = 30.sp,
      modifier = Modifier.padding(top = 16.dp),
    )
    Text(now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", ID)), color = Mist, fontSize = 26.sp, modifier = Modifier.padding(top = 8.dp))
    if (data?.city != null) {
      Spacer(Modifier.height(80.dp))
      Cells(data, now)
    }
  }
}

/** Afnon's prayer cells: past ones fade, the next one is a gold frame instead of a fill. */
@Composable
private fun Cells(data: ScreensaverData, now: ZonedDateTime) {
  // show the day the next prayer falls on: after Isya (or a stale stored day) that is tomorrow
  val next = PrayerClock.next(data.today, data.tomorrow, now)
  val today = if (next.at.toLocalDate() == data.today.date) data.today else data.tomorrow
  val shape = RoundedCornerShape(14.dp)
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    Prayer.entries.forEach { p ->
      val at = today.times[p]
      val isNext = p == next.prayer
      val past = !isNext && at != null && !today.date.atTime(at).atZone(now.zone).isAfter(now)
      val text = when {
        isNext -> Gold
        past -> Mist
        else -> Ivory
      }
      Column(
        Modifier
          .weight(1f)
          .height(156.dp)
          .then(
            when {
              isNext -> Modifier.border(3.dp, Gold, shape)
              past -> Modifier.background(SurfacePast, shape)
              else -> Modifier.background(Surface, shape)
            },
          )
          .padding(horizontal = 20.dp, vertical = 16.dp),
      ) {
        // a word, not only gold: marks the next prayer without relying on colour alone
        Text(if (isNext) "BERIKUT" else "", color = Gold, fontSize = 18.sp, letterSpacing = 0.12.em, modifier = Modifier.weight(1f))
        Text(p.label, color = text, fontSize = 26.sp, fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Normal)
        Text(
          at?.format(TIME) ?: "--:--",
          color = text,
          fontSize = 38.sp,
          fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Normal,
          style = NUM,
        )
      }
    }
  }
  if (today.estimated) {
    Text("perkiraan — jadwal Kemenag belum tersedia", color = Mist, fontSize = 22.sp, modifier = Modifier.padding(top = 20.dp))
  }
}

/** Tabular (civil) Hijri date; may differ by a day from Indonesian moon sighting. */
private fun hijri(): String {
  // the calendar keyword makes ICU use Islamic month names; a plain id_ID locale prints Gregorian ones
  val fmt = SimpleDateFormat("d MMMM y 'H'", ULocale("id_ID@calendar=islamic-civil"))
  return fmt.format(Date())
}
