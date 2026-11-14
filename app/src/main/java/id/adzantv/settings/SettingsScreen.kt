package id.adzantv.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import id.adzantv.data.City
import id.adzantv.dream.hijri
import id.adzantv.time.Prayer
import id.adzantv.time.PrayerTimes
import id.adzantv.time.ReminderClock
import id.adzantv.ui.Display
import id.adzantv.ui.Mihrab
import id.adzantv.ui.Palette
import id.adzantv.ui.SettingsColors as C
import kotlinx.coroutines.delay
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val TIME = DateTimeFormatter.ofPattern("HH:mm")
private val DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID"))

/** Everything the settings screens show. */
internal class SettingsState(
  val cities: List<City>?, // null = loading
  val city: String?,
  val today: PrayerTimes?,
  val message: Message?,
  val busy: Boolean,
  val focusSearch: Boolean,
  val palette: Palette,
  val dreamOn: Boolean,
  val adhanOn: Boolean,
  val reminderOn: Boolean,
  val reminderMinutes: Int,
  val reminderPrayers: Set<Prayer>,
)

/** Everything the settings screens can do. */
internal class SettingsActions(
  val retry: () -> Unit,
  val redetect: () -> Unit,
  val pickCity: (City) -> Unit,
  val testAdhan: () -> Unit,
  val pickPalette: (Palette) -> Unit,
  val toggleDream: () -> Unit,
  val toggleAdhan: () -> Unit,
  val toggleReminder: () -> Unit,
  val setReminderMinutes: (Int) -> Unit,
  val toggleReminderPrayer: (Prayer?) -> Unit, // null = "Semua shalat"
  val previewReminder: () -> Unit,
)

private enum class Sub { CITY, REMINDER, THEME }

/** Setelan, and the screen opened from one of its rows. An open screen replaces the rows: hidden rows would still take D-pad focus. */
@Composable
internal fun SettingsScreens(s: SettingsState, a: SettingsActions) {
  var open by remember { mutableStateOf<Sub?>(null) }
  var last by remember { mutableStateOf(Sub.CITY) }
  val rows = remember { Sub.entries.associateWith { FocusRequester() } }
  // start on the first row; closing a screen returns focus to the row that opened it, so Back never strands the D-pad
  LaunchedEffect(open) { if (open == null) rows.getValue(last).requestFocus() else last = open!! }
  // detection failed and no city yet: go straight to the city list
  LaunchedEffect(s.focusSearch, s.cities) { if (s.focusSearch && !s.cities.isNullOrEmpty()) open = Sub.CITY }

  Box(Modifier.fillMaxSize().background(C.Ground)) {
    when (open) {
      Sub.CITY -> CityScreen(s.cities.orEmpty(), s.city, s.busy, onClose = { open = null }, onRedetect = { a.redetect(); open = null }) {
        a.pickCity(it)
        open = null
      }
      Sub.REMINDER -> ReminderScreen(s, a, onClose = { open = null })
      Sub.THEME -> ThemeScreen(s, onClose = { open = null }) {
        a.pickPalette(it)
        open = null
      }
      null -> Home(s, a, rows) { open = it }
    }
  }
}

@Composable
private fun Home(s: SettingsState, a: SettingsActions, rows: Map<Sub, FocusRequester>, onOpen: (Sub) -> Unit) {
  Sidebar(s.today, s.city)
  Box(Modifier.offset(592.dp, 56.dp).size(2.dp, 968.dp).background(C.Edge))
  Column(Modifier.offset(656.dp, 72.dp).width(1168.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    val offline = s.cities?.isEmpty() == true
    SettingRow(
      PIN,
      "Kota",
      Modifier.focusRequester(rows.getValue(Sub.CITY)),
      hint = if (offline) "Tidak ada koneksi internet. Tekan OK untuk memuat ulang" else null,
      value = s.city ?: "Belum diatur",
      chevron = true,
    ) { if (offline) a.retry() else if (s.cities != null) onOpen(Sub.CITY) }
    SettingRow(
      BELL,
      "Adzan otomatis",
      hint = if (s.adhanOn) "Berbunyi di setiap waktu shalat wajib" else "Tidak berbunyi. Jadwal tetap tampil",
      toggle = s.adhanOn,
    ) { a.toggleAdhan() }
    SettingRow(
      ALARM,
      "Pengingat",
      Modifier.focusRequester(rows.getValue(Sub.REMINDER)),
      hint = if (s.reminderOn) prayersLabel(s.reminderPrayers) else "Tidak ada pengingat sebelum adzan",
      value = if (s.reminderOn) "${s.reminderMinutes} menit sebelum" else "Mati",
      chevron = true,
    ) { onOpen(Sub.REMINDER) }
    SettingRow(PLAY, "Tes adzan", hint = "Berbunyi 5 detik setelah ditekan") { a.testAdhan() }
    SettingRow(
      MOON,
      "Screensaver Adzan TV",
      hint = if (s.dreamOn) "Tampil saat TV diam" else "Memakai screensaver bawaan TV",
      toggle = s.dreamOn,
    ) { a.toggleDream() }
    SettingRow(
      PALETTE,
      "Tema warna",
      Modifier.focusRequester(rows.getValue(Sub.THEME)),
      hint = "Untuk screensaver dan pengingat",
      value = s.palette.name,
      chevron = true,
    ) { onOpen(Sub.THEME) }
  }
  (s.message ?: if (s.cities == null) Message("Memuat daftar kota…", ok = false) else null)?.let { Status(it, Modifier.offset(656.dp, 852.dp)) }
  Hints(Modifier.offset(656.dp, 968.dp), "Panah" to "pindah", "OK" to "pilih", "Kembali" to "keluar")
}

internal fun prayersLabel(prayers: Set<Prayer>) =
  if (prayers.containsAll(ReminderClock.ALL)) "Semua shalat" else "${prayers.size} dari ${ReminderClock.ALL.size} shalat"

/** The clock and both dates, so the TV's time can be checked against today's times below it. */
@Composable
private fun Sidebar(today: PrayerTimes?, city: String?) {
  var now by remember { mutableStateOf(ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES)) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(1000)
      now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES) // an unchanged minute recomposes nothing
    }
  }
  Row(Modifier.offset(96.dp, 56.dp), verticalAlignment = Alignment.CenterVertically) {
    Mihrab(Modifier.size(40.dp, 60.dp), line = C.Dim, stroke = 2.dp, fill = C.Row, inner = C.Edge)
    Spacer(Modifier.width(20.dp))
    Text("Setelan", color = C.Text, fontSize = 52.sp, fontFamily = Display, fontWeight = FontWeight.Bold)
  }
  Column(Modifier.offset(96.dp, 146.dp).width(424.dp)) {
    Text(now.format(TIME), color = C.Text, fontSize = 100.sp, fontFamily = Display, lineHeight = 1.em, style = TextStyle(fontFeatureSettings = "tnum"))
    Text(hijri(), color = C.Text, fontSize = 30.sp)
    Text(now.format(DATE), color = C.Dim, fontSize = 26.sp)
    Spacer(Modifier.height(28.dp))
    Row(Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.Bottom) {
      Text("Jadwal hari ini", color = C.Dim, fontSize = 26.sp, modifier = Modifier.weight(1f))
      if (city != null) Text(city, color = C.Dim, fontSize = 26.sp, maxLines = 1)
    }
    if (today == null) {
      Text("Memuat jadwal…", color = C.Dim, fontSize = 30.sp)
    } else {
      val time = now.toLocalTime()
      val next = Prayer.entries.firstOrNull { p -> p.obligatory && today.times[p]?.isAfter(time) == true }
      val shape = RoundedCornerShape(18.dp)
      Column(Modifier.fillMaxWidth().background(C.Row, shape).border(2.dp, C.Edge, shape)) {
        Prayer.entries.forEachIndexed { i, p ->
          val isNext = p == next
          // past times dim, the next one is marked: the eye lands on what is still to come
          val color = if (isNext) C.On else if (today.times[p]?.isAfter(time) == false) C.Dim else C.Text
          if (i > 0 && !isNext && Prayer.entries[i - 1] != next) {
            Box(Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(2.dp).background(C.Edge))
          }
          Row(
            Modifier
              .fillMaxWidth()
              .height(76.dp)
              .then(if (isNext) Modifier.border(4.dp, C.On, RoundedCornerShape(16.dp)) else Modifier)
              .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            val weight = if (isNext) FontWeight.SemiBold else FontWeight.Normal
            Text(p.label, color = color, fontSize = 32.sp, fontWeight = weight, modifier = Modifier.weight(1f))
            // a word, not only colour: marks the next prayer
            if (isNext) Text("berikutnya", color = C.On, fontSize = 26.sp, modifier = Modifier.padding(end = 14.dp))
            Text(today.times[p]?.format(TIME) ?: "--:--", color = color, fontSize = 36.sp, fontWeight = weight)
          }
        }
      }
      Text(
        if (today.estimated) "Perkiraan, bukan jadwal Kemenag" else "Sumber: Kemenag RI",
        color = C.Dim,
        fontSize = 26.sp,
        maxLines = 1,
        modifier = Modifier.padding(top = 14.dp),
      )
    }
  }
}
