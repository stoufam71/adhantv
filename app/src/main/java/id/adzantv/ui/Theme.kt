package id.adzantv.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** Afnon's "Mihrab" palette: lapis ground, lapis-800 surfaces, gold lines, plaster text. */
val LapisDeep = Color(0xFF0A1730) // lapis-950, the ground
val Lapis = Color(0xFF12264A) // lapis-800, the arch fill and cells
val Gold = Color(0xFFD8B46A)
val GoldLight = Color(0xFFF0D79C) // gold-200, the call screen's arch edge
val Ivory = Color(0xFFEFE7D6) // plaster-100
val ArchInner = Color(0x73D8B46A) // the arch's inner line, gold at 45%
val PatternLine = Color(0x24D8B46A) // khatam lattice, gold at 14%: Afnon uses 7%, too faint to read on a TV across the room

/** Secondary text on lapis (4.5:1 on lapis-800). */
val Mist = Color(0xFF9FB0CC)
val LineSoft = Color(0x40EFE7D6)

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
