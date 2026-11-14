package id.adzantv.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The settings screens' own fixed colours. They never follow the chosen [Palette], so settings read the same
 * whatever theme colours the screensaver, the adhan call and the reminder. Two colours carry meaning:
 * [Focus] is where the remote is, [On] is on / chosen / next; each state also has a word or a mark.
 */
object SettingsColors {
  val Ground = Color(0xFF0F1114)
  val Row = Color(0xFF1C2026)
  val Edge = Color(0xFF6E7682) // row outline, 4.1:1 on Ground
  val Text = Color(0xFFF3EFE7) // 14.3:1 on Row
  val Dim = Color(0xFFC0C5CC) // 9.4:1 on Row
  val Focus = Color(0xFFECE9E2) // a plain light row with Ground text
  val On = Color(0xFF7BDCA2) // 9.8:1 on Row
}

/** Material3 for the city search field, in the fixed colours. */
val SettingsScheme = darkColorScheme(
  primary = SettingsColors.Text,
  onPrimary = SettingsColors.Ground,
  background = SettingsColors.Ground,
  onBackground = SettingsColors.Text,
  surface = SettingsColors.Ground,
  onSurface = SettingsColors.Text,
  onSurfaceVariant = SettingsColors.Dim,
  outline = SettingsColors.Edge,
  surfaceVariant = SettingsColors.Row,
)
