package id.adzantv.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** Afnon-like "Mihrab" palette: lapis and gold. */
val Lapis = Color(0xFF0E2A5C)
val LapisDeep = Color(0xFF061631)
val Gold = Color(0xFFD4AF37)
val Ivory = Color(0xFFF3EBD3)

/** Screensaver tones from Afnon: warm grey on black, so a dark room is not hit by a white clock. */
val ScreensaverText = Color(0xFFA69F90)
val ScreensaverDim = Color(0xFF7D7769)
val ScreensaverNext = Color(0xFFB8A578)
val ArchLine = Color(0x59D8B46A) // Afnon's screensaver arch line, gold at 35%

/** Secondary text on lapis; ScreensaverDim fails 4.5:1 on the adhan card's blue. */
val Mist = Color(0xFF9FB0CC)
val LineSoft = Color(0x59F3EBD3)

/** Afnon sets its display type in Marcellus; the system serif is the closest weight without shipping a font. */
val Display = FontFamily.Serif

/** Material3 defaults are a light purple theme, which makes the city search field unreadable on lapis. */
val TvColors = darkColorScheme(
  primary = Gold,
  onPrimary = LapisDeep,
  background = LapisDeep,
  onBackground = Ivory,
  surface = LapisDeep,
  onSurface = Ivory,
  onSurfaceVariant = Mist,
  outline = LineSoft,
  surfaceVariant = Lapis,
)
