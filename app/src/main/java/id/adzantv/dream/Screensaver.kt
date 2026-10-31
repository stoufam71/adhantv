package id.adzantv.dream

import android.icu.text.SimpleDateFormat
import android.icu.util.IslamicCalendar
import android.icu.util.ULocale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.adzantv.time.Prayer
import id.adzantv.time.PrayerClock
import id.adzantv.time.PrayerTimes
import id.adzantv.ui.Gold
import id.adzantv.ui.Ivory
import id.adzantv.ui.Lapis
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.sin

data class ScreensaverData(val city: String?, val today: PrayerTimes, val tomorrow: PrayerTimes)

private val ID = Locale.forLanguageTag("id-ID")
private val Dim = Ivory.copy(alpha = 0.55f)

@Composable
fun Screensaver(data: ScreensaverData?) {
  var now by remember { mutableStateOf(ZonedDateTime.now()) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(1000)
      now = ZonedDateTime.now()
    }
  }
  // slow drift against burn-in: a new spot every minute
  val minute = now.toEpochSecond() / 60
  val dx = (sin(minute * 1.3) * 60).dp
  val dy = (sin(minute * 0.7) * 40).dp

  Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
    Column(
      Modifier
        .offset(dx, dy)
        .border(2.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(topStartPercent = 50, topEndPercent = 50))
        .background(Lapis.copy(alpha = 0.25f), RoundedCornerShape(topStartPercent = 50, topEndPercent = 50))
        .padding(horizontal = 72.dp, vertical = 40.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(data?.city ?: "", color = Dim, fontSize = 20.sp)
      Text(now.format(DateTimeFormatter.ofPattern("HH:mm")), color = Ivory.copy(alpha = 0.85f), fontSize = 112.sp, fontWeight = FontWeight.Light)
      Text(now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", ID)) + "  ·  " + hijri(), color = Dim, fontSize = 20.sp)
      Spacer(Modifier.height(28.dp))
      when {
        data == null -> {}
        data.city == null -> Text("Atur kota di aplikasi Adzan TV", color = Gold, fontSize = 24.sp)
        else -> Times(data, now)
      }
    }
  }
}

@Composable
private fun Times(data: ScreensaverData, now: ZonedDateTime) {
  // the stored day can go stale past midnight; tomorrow then becomes today
  val today = if (data.today.date < now.toLocalDate()) data.tomorrow else data.today
  val next = PrayerClock.next(data.today, data.tomorrow, now)
  val left = Duration.between(now, next.at)
  Text(
    "${next.prayer.label} dalam %d:%02d:%02d".format(left.toHours(), left.toMinutesPart(), left.toSecondsPart()),
    color = Gold,
    fontSize = 30.sp,
  )
  Spacer(Modifier.height(20.dp))
  Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
    Prayer.entries.forEach { p ->
      val isNext = p == next.prayer && next.at.toLocalDate() == today.date
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(p.label, color = if (isNext) Gold else Dim, fontSize = 18.sp)
        Text(
          today.times[p]?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "--:--",
          color = if (isNext) Gold else Ivory.copy(alpha = 0.75f),
          fontSize = 30.sp,
          fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
        )
      }
    }
  }
  if (today.estimated) {
    Spacer(Modifier.height(16.dp))
    Row {
      Text("perkiraan", color = Dim, fontSize = 14.sp)
      Spacer(Modifier.width(6.dp))
      Text("(jadwal Kemenag belum tersedia)", color = Dim, fontSize = 14.sp)
    }
  }
}

/** Tabular (civil) Hijri date; may differ by a day from Indonesian moon sighting. */
private fun hijri(): String {
  val fmt = SimpleDateFormat("d MMMM y 'H'", ULocale("id_ID"))
  fmt.calendar = IslamicCalendar().apply { calculationType = IslamicCalendar.CalculationType.ISLAMIC_CIVIL }
  return fmt.format(Date())
}
