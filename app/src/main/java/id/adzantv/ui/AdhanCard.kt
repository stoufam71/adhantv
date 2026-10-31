package id.adzantv.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Afnon's call screen: a larger, brighter-edged mihrab centred on the lattice, running off the bottom. */
@Composable
fun AdhanCard(prayerLabel: String, at: LocalTime, city: String?) {
  Stage {
    Mihrab(Modifier.offset(600.dp, 110.dp).size(720.dp, 1080.dp), line = GoldLight, stroke = 6.dp)
    Column(Modifier.offset(640.dp, 230.dp).width(640.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text("حَيَّ عَلَى الصَّلَاةِ", color = Gold, fontSize = 54.sp, lineHeight = 1.4.em, fontFamily = Display)
      Text("WAKTUNYA SHALAT", color = Mist, fontSize = 22.sp, letterSpacing = 0.12.em, modifier = Modifier.padding(top = 32.dp))
      Text(prayerLabel, color = Ivory, fontSize = 128.sp, lineHeight = 1.1.em, fontFamily = Display, fontWeight = FontWeight.Bold)
      Text(
        at.format(DateTimeFormatter.ofPattern("HH:mm")),
        color = Gold,
        fontSize = 64.sp,
        fontFamily = Display,
        style = TextStyle(fontFeatureSettings = "tnum"),
      )
      if (city != null) Text(city, color = Mist, fontSize = 26.sp, modifier = Modifier.padding(top = 8.dp))
      Text(
        "Tekan OK atau Kembali untuk menghentikan",
        color = Mist,
        fontSize = 22.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 48.dp),
      )
    }
  }
}
