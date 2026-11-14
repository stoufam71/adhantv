package id.adzantv.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.adzantv.dream.Screensaver
import id.adzantv.dream.ScreensaverData
import id.adzantv.ui.LocalPalette
import id.adzantv.ui.Palette
import id.adzantv.ui.Palettes
import id.adzantv.ui.SettingsColors as C

/**
 * Each palette as a live, scaled-down screensaver, two by two; OK picks, Back keeps the current one.
 * Only the previews wear a theme: this screen itself stays in the fixed settings colours.
 */
@Composable
internal fun ThemeScreen(s: SettingsState, onClose: () -> Unit, onPick: (Palette) -> Unit) {
  BackHandler(onBack = onClose)
  val start = remember { FocusRequester() }
  LaunchedEffect(Unit) { start.requestFocus() }
  // the real screensaver, with today's times when they are loaded
  val preview = s.today?.let { ScreensaverData(s.city, it, it.copy(date = it.date.plusDays(1)), s.adhanOn) }
  Column(Modifier.offset(340.dp, 40.dp).width(1240.dp)) {
    Title("Tema warna")
    Text("Warna untuk screensaver, adzan dan pengingat. Setelan tidak berubah.", color = C.Dim, fontSize = 28.sp)
    Spacer(Modifier.height(16.dp))
    Palettes.chunked(2).forEach { row ->
      Row(horizontalArrangement = Arrangement.spacedBy(40.dp), modifier = Modifier.padding(bottom = 16.dp)) {
        row.forEach { p ->
          val isCurrent = p.id == s.palette.id
          FocusBox(Modifier.width(600.dp).then(if (isCurrent) Modifier.focusRequester(start) else Modifier), onClick = { onPick(p) }) { focused ->
            val fg = if (focused) C.Ground else C.Text
            Column {
              Box(Modifier.padding(6.dp).fillMaxWidth().height(318.dp).clip(RoundedCornerShape(12.dp))) {
                CompositionLocalProvider(LocalPalette provides p) { Screensaver(preview) }
              }
              Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(p.name, color = fg, fontSize = 32.sp, fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
                // a word and a mark, not only colour: the palette in use
                if (isCurrent) {
                  Icon(CHECK, if (focused) C.Ground else C.On, stroke = 2.4f)
                  Text("dipakai", color = if (focused) C.Ground else C.On, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 14.dp))
                }
              }
            }
          }
        }
      }
    }
  }
  Hints(Modifier.offset(340.dp, 984.dp), "Panah" to "pindah", "OK" to "pakai", "Kembali" to "batal")
}
