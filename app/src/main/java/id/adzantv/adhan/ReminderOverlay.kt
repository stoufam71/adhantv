package id.adzantv.adhan

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import id.adzantv.data.Prefs
import id.adzantv.time.Prayer
import id.adzantv.ui.LocalPalette
import id.adzantv.ui.REMINDER_HEIGHT
import id.adzantv.ui.REMINDER_WIDTH
import id.adzantv.ui.ReminderCard
import id.adzantv.ui.paletteOf
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The reminder card as a window above whatever is playing. It takes no focus, so the programme and the
 * remote carry on; it removes itself after [SHOW_MS]. Main thread only.
 */
object ReminderOverlay {
  private const val SHOW_MS = 10_000L
  private const val MARGIN = 96 // stage units from the top and right edges
  private val main = Handler(Looper.getMainLooper())
  private var showing: Pair<WindowManager, View>? = null
  private var owner: Owner? = null
  private val hideLater = Runnable { hide() } // one instance, so removeCallbacks finds it

  /** A window is not a LifecycleOwner, so Compose's owners are wired by hand, as in the screensaver. */
  private class Owner : LifecycleOwner, SavedStateRegistryOwner {
    val registry = LifecycleRegistry(this)
    private val saved = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry

    init {
      saved.performRestore(null)
      registry.currentState = Lifecycle.State.RESUMED
    }
  }

  /** Shows the card; false when the "display over other apps" grant is missing or the window is refused. */
  fun show(context: Context, prayer: Prayer, prayerAt: Long, minutes: Int): Boolean {
    val ctx = context.applicationContext
    if (!Settings.canDrawOverlays(ctx)) {
      Log.w("AdzanTV", "reminder skipped: no overlay permission (run tools/install.sh)")
      return false
    }
    hide()
    val wm = ctx.getSystemService(WindowManager::class.java)
    val prefs = Prefs(ctx)
    val time = Instant.ofEpochMilli(prayerAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
    // the same mapping as Stage: 1920 stage units across the screen's width
    val metrics = ctx.resources.displayMetrics
    val scale = metrics.widthPixels / 1920f
    val lifecycle = Owner()
    val view = ComposeView(ctx).apply {
      setViewTreeLifecycleOwner(lifecycle)
      setViewTreeSavedStateRegistryOwner(lifecycle)
      setContent {
        CompositionLocalProvider(
          LocalPalette provides paletteOf(prefs.palette),
          LocalDensity provides Density(scale, ctx.resources.configuration.fontScale),
        ) {
          ReminderCard(prayer.label, minutes, time, prefs.cityName, prefs.adhanOn)
        }
      }
    }
    val params = WindowManager.LayoutParams(
      (REMINDER_WIDTH * scale).toInt(),
      (REMINDER_HEIGHT * scale).toInt(),
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
      // not focusable, so the remote stays with the programme. Deliberately still touchable: Android 12+ forces a
      // touch-through overlay to 80% opacity, and a TV has no touches to block
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
      PixelFormat.TRANSLUCENT,
    ).apply {
      gravity = Gravity.TOP or Gravity.END
      x = (MARGIN * scale).toInt()
      y = (MARGIN * scale).toInt()
      // the system's own fade; it honours the "remove animations" setting
      windowAnimations = android.R.style.Animation_Toast
    }
    return try {
      wm.addView(view, params)
      showing = wm to view
      owner = lifecycle
      main.postDelayed(hideLater, SHOW_MS)
      true
    } catch (e: Exception) {
      Log.e("AdzanTV", "reminder window refused", e)
      false
    }
  }

  private fun hide() {
    main.removeCallbacks(hideLater)
    showing?.let { (wm, view) -> runCatching { wm.removeView(view) } }
    owner?.registry?.currentState = Lifecycle.State.DESTROYED
    showing = null
    owner = null
  }
}
