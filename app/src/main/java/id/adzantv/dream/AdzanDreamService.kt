package id.adzantv.dream

import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.util.Log
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import id.adzantv.Adzan
import id.adzantv.data.Prefs
import id.adzantv.adhan.AdhanPlayer
import id.adzantv.time.Prayer
import id.adzantv.ui.AdhanCard
import id.adzantv.ui.LocalPalette
import id.adzantv.ui.paletteOf
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.concurrent.thread

/** The system screensaver. A DreamService is not a LifecycleOwner, so Compose's owners are wired by hand. */
class AdzanDreamService : DreamService(), LifecycleOwner, SavedStateRegistryOwner {
  private val registry = LifecycleRegistry(this)
  private val savedState = SavedStateRegistryController.create(this)
  override val lifecycle: Lifecycle get() = registry
  override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

  private val data = mutableStateOf<ScreensaverData?>(null)
  private val adhan = mutableStateOf<Pair<String, LocalTime>?>(null) // prayer label, time
  private var player: AdhanPlayer? = null

  override fun onCreate() {
    super.onCreate()
    savedState.performRestore(null)
    registry.currentState = Lifecycle.State.CREATED
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    isInteractive = false // any key dismisses (R4)
    isFullscreen = true
    window.decorView.setViewTreeLifecycleOwner(this)
    window.decorView.setViewTreeSavedStateRegistryOwner(this)
    setContentView(
      ComposeView(this).apply {
        val palette = paletteOf(Prefs(this@AdzanDreamService).palette)
        setContent {
          CompositionLocalProvider(LocalPalette provides palette) {
            val call = adhan.value
            if (call != null) AdhanCard(call.first, call.second, data.value?.city, progress = { player?.progress() }) else Screensaver(data.value)
          }
        }
      },
    )
  }

  override fun onDreamingStarted() {
    super.onDreamingStarted()
    current = this
    registry.currentState = Lifecycle.State.RESUMED
    load()
  }

  private fun load() {
    val main = Handler(Looper.getMainLooper())
    thread {
      try {
        val prefs = Prefs(this)
        val today = LocalDate.now()
        val d = ScreensaverData(
          city = prefs.cityName,
          today = Adzan.day(this, today),
          tomorrow = Adzan.day(this, today.plusDays(1)),
          adhanOn = prefs.adhanOn,
        )
        main.post { data.value = d }
        // a long-running screensaver must not outlive tomorrow's data
        main.postDelayed({ if (current === this) load() }, 3_600_000)
        Adzan.reschedule(this) // cheap self-heal of the alarm chain
      } catch (e: Exception) {
        Log.e("AdzanTV", "screensaver load failed", e)
      }
    }
  }

  /** Main thread. The call plays inside the screensaver; any key ends both (R4). */
  fun showAdhan(prayerName: String, atMillis: Long) {
    val label = runCatching { Prayer.valueOf(prayerName).label }.getOrDefault("Adzan")
    adhan.value = label to Instant.ofEpochMilli(atMillis).atZone(ZoneId.systemDefault()).toLocalTime()
    player?.stop()
    player = AdhanPlayer(this) { endAdhan() }.also { it.start() }
  }

  private fun endAdhan() {
    player?.stop()
    player = null
    adhan.value = null
  }

  override fun onDreamingStopped() {
    endAdhan()
    if (current === this) current = null
    registry.currentState = Lifecycle.State.CREATED
    super.onDreamingStopped()
  }

  override fun onDestroy() {
    if (current === this) current = null
    registry.currentState = Lifecycle.State.DESTROYED
    super.onDestroy()
  }

  companion object {
    /** The running screensaver, so the adhan plays inside it instead of ending it. Main thread only. */
    @Volatile var current: AdzanDreamService? = null
  }
}
