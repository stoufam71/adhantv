package id.adzantv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A 1920×1080 stage on Afnon's lattice ground, scaled to fit any TV, so content can be placed with
 * Afnon's own pixel coordinates as dp. [dx]/[dy] shift everything, ornament included, against burn-in.
 * [quiet] is a stage point (in dp) where the ornament fades, so text placed there stays clean.
 */
@Composable
fun Stage(dx: Dp = 0.dp, dy: Dp = 0.dp, quiet: Offset? = null, content: @Composable BoxScope.() -> Unit) {
  BoxWithConstraints(Modifier.fillMaxSize().background(Ground), contentAlignment = Alignment.Center) {
    val scale = minOf(maxWidth / 1920.dp, maxHeight / 1080.dp)
    val outer = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(outer.density * scale, outer.fontScale)) {
      Box(Modifier.requiredSize(1920.dp, 1080.dp).offset(dx, dy)) {
        // overdraw past the stage edges so the drift never exposes a bare border
        IslamicPattern(
          Modifier.requiredSize(2112.dp, 1272.dp).align(Alignment.Center),
          fade = quiet?.let { Offset((it.x + 96) / 2112, (it.y + 96) / 1272) },
        )
        content()
      }
    }
  }
}
