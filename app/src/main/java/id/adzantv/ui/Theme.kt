package id.adzantv.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily

/**
 * A dark-room palette: a very dark ground and warm, dimmed text, never pure white.
 * The four were compared side by side in docs/design/palette-sketch.html.
 */
class Palette(
  val id: String,
  val name: String,
  val ground: Color,
  val surface: Color,
  val line: Color, // arch outline
  val accent: Color, // next prayer, focus fill
  val text: Color,
  val dim: Color, // secondary text, at least 4.5:1 on surface
  val ornament: Color = line,
)

val Palettes = listOf(
  // the Kaaba's black cover with its gold embroidery
  Palette("kiswah", "Kiswah", Color(0xFF0B0A08), Color(0xFF17140F), Color(0xFFB8964F), Color(0xFFD9B76A), Color(0xFFD8CDB4), Color(0xFF8C8370)),
  // the green dome of Masjid Nabawi at night, with brass
  Palette("nabawi", "Hijau Nabawi", Color(0xFF071A15), Color(0xFF0E2A22), Color(0xFFB8964F), Color(0xFFD9B76A), Color(0xFFDCD3BC), Color(0xFF8FA89C)),
  // Ottoman Iznik tiles: midnight teal, turquoise and gold
  Palette("iznik", "Teal Iznik", Color(0xFF061A1C), Color(0xFF0D2A2D), Color(0xFFB8964F), Color(0xFF4FB3A9), Color(0xFFDAD6C8), Color(0xFF86A3A0), ornament = Color(0xFF4FB3A9)),
  // Persian lapis and gold, darkened for night
  Palette("lapis", "Lapis Malam", Color(0xFF060E1F), Color(0xFF0D1C38), Color(0xFFB8964F), Color(0xFFD9B76A), Color(0xFFD8CFBA), Color(0xFF8593AD)),
)

fun paletteOf(id: String?): Palette = Palettes.firstOrNull { it.id == id } ?: Palettes[0]

/** The palette every screen draws with; each entry point provides the one saved in Prefs. */
val LocalPalette = staticCompositionLocalOf { Palettes[0] }

// Semantic tokens, read from the current palette so screens need not know which one is active.
val Ground: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.ground
val Surface: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surface

/** Surface at 60% over Ground, solid so the ornament does not show through a faded cell. */
val SurfacePast: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.let { lerp(it.ground, it.surface, 0.6f) }
val Gold: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.accent
val GoldLine: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.line

/** The arch edge when a prayer is near, and on the call screen. */
val GoldLight: Color @Composable @ReadOnlyComposable get() = lerp(LocalPalette.current.accent, Color.White, 0.35f)
val Ivory: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.text
val ArchInner: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.line.copy(alpha = 0.55f)

/** The ornament at 9%: texture, never competing with text. */
val PatternLine: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.ornament.copy(alpha = 0.09f)
val Mist: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.dim
val LineSoft: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.text.copy(alpha = 0.25f)

/** Afnon sets its display type in Marcellus; the system serif is the closest weight without shipping a font. */
val Display = FontFamily.Serif

/** Material3 defaults are a light purple theme, which makes the city search field unreadable on a dark ground. */
val TvColors: ColorScheme
  @Composable @ReadOnlyComposable get() = darkColorScheme(
    primary = Gold,
    onPrimary = Ground,
    background = Ground,
    onBackground = Ivory,
    surface = Ground,
    onSurface = Ivory,
    onSurfaceVariant = Mist,
    outline = LineSoft,
    surfaceVariant = Surface,
  )
