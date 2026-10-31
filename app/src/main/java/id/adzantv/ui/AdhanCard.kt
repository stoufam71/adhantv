package id.adzantv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun AdhanCard(prayerLabel: String, at: LocalTime, city: String?) {
  BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Lapis, LapisDeep)))) {
    // whisper-quiet khatam lattice behind everything, like Afnon's pattern layer
    IslamicPattern(Modifier.fillMaxSize(), color = Gold.copy(alpha = 0.07f))
    // Afnon's call screen: a double-line arch that runs off the bottom, name in plaster, time in gold
    val archW = maxWidth * 0.38f
    val archH = maxHeight * 1.05f
    val archModifier = Modifier.align(Alignment.TopCenter).offset(y = maxHeight * 0.14f)
    Mihrab(archModifier.width(archW).height(archH), color = Gold.copy(alpha = 0.5f), stroke = 2.dp)
    Mihrab(
      archModifier.width(archW * 0.94f).height(archH * 0.94f),
      color = Gold.copy(alpha = 0.4f),
    )

    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
      Text("WAKTUNYA SHALAT", color = Mist, fontSize = 16.sp, letterSpacing = 0.2.em)
      Spacer(Modifier.height(8.dp))
      Text(prayerLabel, color = Ivory, fontSize = 64.sp, fontFamily = Display)
      Text(
        at.format(DateTimeFormatter.ofPattern("HH:mm")),
        color = Gold,
        fontSize = 32.sp,
        fontFamily = Display,
        style = TextStyle(fontFeatureSettings = "tnum"),
      )
      Spacer(Modifier.height(8.dp))
      if (city != null) Text(city, color = Mist, fontSize = 16.sp)
      Spacer(Modifier.height(28.dp))
      Text("Tekan OK atau Kembali untuk menghentikan", color = Mist, fontSize = 16.sp)
    }
  }
}
