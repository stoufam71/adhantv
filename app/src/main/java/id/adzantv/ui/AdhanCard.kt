package id.adzantv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun AdhanCard(prayerLabel: String, at: LocalTime, city: String?) {
  Box(
    Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Lapis, LapisDeep))),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      Modifier
        .border(3.dp, Gold, RoundedCornerShape(topStartPercent = 50, topEndPercent = 50))
        .padding(horizontal = 96.dp, vertical = 56.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text("Waktunya shalat", color = Ivory, fontSize = 28.sp)
      Spacer(Modifier.height(12.dp))
      Text(prayerLabel, color = Gold, fontSize = 88.sp, fontWeight = FontWeight.Bold)
      Text(at.format(DateTimeFormatter.ofPattern("HH:mm")), color = Ivory, fontSize = 40.sp)
      if (city != null) Text(city, color = Ivory.copy(alpha = 0.7f), fontSize = 22.sp)
      Spacer(Modifier.height(32.dp))
      Text("Tekan Kembali untuk menghentikan", color = Ivory.copy(alpha = 0.6f), fontSize = 18.sp)
    }
  }
}
