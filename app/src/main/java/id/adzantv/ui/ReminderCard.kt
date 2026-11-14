package id.adzantv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Stage width of the card; the overlay window and the settings sample both size it from this. */
const val REMINDER_WIDTH = 820
const val REMINDER_HEIGHT = 178

/**
 * The reminder shown shortly before a prayer, in the chosen theme. Sizes are stage units (1920 wide), so the
 * caller provides a density that maps the stage onto the screen. [adhanOn] off drops the word "Adzan": none will sound.
 */
@Composable
fun ReminderCard(prayerLabel: String, minutes: Int, time: String, city: String?, adhanOn: Boolean, modifier: Modifier = Modifier) {
  val shape = RoundedCornerShape(22.dp)
  val dim = lerp(Mist, Ivory, 0.45f) // the palette's dim text is too faint to read across a room
  Row(
    modifier
      .size(REMINDER_WIDTH.dp, REMINDER_HEIGHT.dp)
      .background(Surface, shape)
      .border(3.dp, Gold, shape)
      .padding(horizontal = 36.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Mihrab(Modifier.size(60.dp, 90.dp), stroke = 3.dp)
    Spacer(Modifier.width(32.dp))
    Column {
      Text("Pengingat shalat", color = dim, fontSize = 26.sp)
      Text("$prayerLabel $minutes menit lagi", color = Ivory, fontSize = 48.sp, fontFamily = Display, fontWeight = FontWeight.Bold, maxLines = 1)
      Text((if (adhanOn) "Adzan pukul " else "Pukul ") + time + (city?.let { "  ·  $it" } ?: ""), color = dim, fontSize = 26.sp, maxLines = 1)
    }
  }
}
