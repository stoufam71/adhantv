package id.adzantv.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.adzantv.time.Prayer
import id.adzantv.time.PrayerClock
import id.adzantv.time.ReminderClock
import id.adzantv.ui.LocalPalette
import id.adzantv.ui.ReminderCard
import id.adzantv.ui.SettingsColors as C
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * The reminder before adhan: on or off, how many minutes before, and for which prayers. Every change is
 * saved at once. Left column top to bottom, then Down or Right into the prayer list.
 */
@Composable
internal fun ReminderScreen(s: SettingsState, a: SettingsActions, onClose: () -> Unit) {
  BackHandler(onBack = onClose)
  val start = remember { FocusRequester() }
  val prayers = remember { FocusRequester() }
  LaunchedEffect(Unit) { start.requestFocus() }
  val on = s.reminderOn

  Column(Modifier.offset(96.dp, 40.dp).width(820.dp)) {
    Title("Pengingat")
    Text("Muncul di layar sebelum adzan, tanpa suara", color = C.Dim, fontSize = 28.sp)
    Spacer(Modifier.height(20.dp))
    SettingRow(ALARM, "Pengingat", Modifier.focusRequester(start), toggle = on) { a.toggleReminder() }
    Spacer(Modifier.height(20.dp))
    Stepper(s.reminderMinutes, on, a.setReminderMinutes)
    Spacer(Modifier.height(28.dp))
    Text("Contoh. Tekan OK untuk mencoba di layar", color = C.Dim, fontSize = 26.sp, modifier = Modifier.padding(bottom = 12.dp))
    // the card itself wears the chosen theme; the light frame is this screen's focus
    FocusBox(Modifier.focusProperties { down = prayers }, onClick = a.previewReminder, radius = 30.dp) {
      Box(Modifier.padding(8.dp)) {
        CompositionLocalProvider(LocalPalette provides s.palette) { Sample(s) }
      }
    }
  }

  Column(Modifier.offset(1004.dp, 124.dp).width(820.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Ingatkan untuk shalat", color = C.Dim, fontSize = 28.sp, modifier = Modifier.padding(bottom = 8.dp))
    val all = s.reminderPrayers.containsAll(ReminderClock.ALL)
    CheckRow("Semua shalat", all, on, Modifier.focusRequester(prayers), note = "bawaan") { a.toggleReminderPrayer(null) }
    ReminderClock.ALL.forEach { p ->
      CheckRow(p.label, p in s.reminderPrayers, on) { a.toggleReminderPrayer(p) }
    }
  }
  s.message?.let { Status(it, Modifier.offset(96.dp, 880.dp)) }
  Hints(Modifier.offset(96.dp, 984.dp), "Panah" to "pindah", "Kiri Kanan" to "ubah menit", "OK" to "pilih", "Kembali" to "selesai")
}

/** One row instead of ten small buttons: Left takes a minute off, Right adds one, between 1 and 10. */
@Composable
private fun Stepper(minutes: Int, enabled: Boolean, onChange: (Int) -> Unit) {
  FocusBox(
    Modifier
      .fillMaxWidth()
      .height(140.dp)
      .onPreviewKeyEvent { e ->
        val step = when (e.key) {
          Key.DirectionLeft -> -1
          Key.DirectionRight -> 1
          else -> 0
        }
        // both key down and key up are consumed, so focus never leaves the row sideways
        if (step != 0 && e.type == KeyEventType.KeyDown) onChange((minutes + step).coerceIn(1, 10))
        step != 0
      },
    onClick = {},
    enabled = enabled,
  ) { focused ->
    val fg = if (focused) C.Ground else C.Text
    Row(Modifier.fillMaxSize().padding(horizontal = 36.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text("Waktu pengingat", color = fg, fontSize = 34.sp, fontWeight = FontWeight.Medium)
        Text("sebelum adzan", color = if (focused) C.Ground else C.Dim, fontSize = 26.sp)
      }
      Arrow(left = true, focused, dim = minutes <= 1)
      Text("$minutes menit", color = fg, fontSize = 52.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(308.dp))
      Arrow(left = false, focused, dim = minutes >= 10)
    }
  }
}

/** The arrow fades at the end of the range, where that key does nothing more. */
@Composable
private fun Arrow(left: Boolean, focused: Boolean, dim: Boolean) {
  Box(
    Modifier
      .size(60.dp)
      .then(if (focused) Modifier.background(C.Ground.copy(alpha = if (dim) 0.3f else 1f), CircleShape) else Modifier.border(2.dp, C.Edge, CircleShape)),
    contentAlignment = Alignment.Center,
  ) {
    Chevron(if (focused) C.Focus else if (dim) C.Edge else C.Text, left)
  }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, enabled: Boolean, modifier: Modifier = Modifier, note: String? = null, onClick: () -> Unit) {
  FocusBox(modifier.fillMaxWidth().height(100.dp), onClick, enabled) { focused ->
    val fg = if (focused) C.Ground else C.Text
    Row(Modifier.fillMaxSize().padding(horizontal = 32.dp), verticalAlignment = Alignment.CenterVertically) {
      CheckBox(checked, focused)
      Spacer(Modifier.width(28.dp))
      Text(label, color = fg, fontSize = 34.sp, fontWeight = if (note != null) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
      if (note != null) Text(note, color = if (focused) C.Ground else C.Dim, fontSize = 26.sp)
    }
  }
}

/** The card as it will appear, for the next prayer when today's times are loaded. */
@Composable
private fun Sample(s: SettingsState) {
  // a schedule loaded days ago has no prayer left after now; fall back to the placeholder instead of crashing
  val next = s.today?.let { runCatching { PrayerClock.next(it, it.copy(date = it.date.plusDays(1)), ZonedDateTime.now()) }.getOrNull() }
  ReminderCard(
    next?.prayer?.label ?: Prayer.ASHAR.label,
    s.reminderMinutes,
    next?.at?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "15:00",
    s.city,
    s.adhanOn,
    Modifier.width(804.dp),
  )
}
