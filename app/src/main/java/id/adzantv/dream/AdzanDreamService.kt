package id.adzantv.dream

import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.util.Log
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
import java.time.LocalDate
import kotlin.concurrent.thread

/** The system screensaver. A DreamService is not a LifecycleOwner, so Compose's owners are wired by hand. */
class AdzanDreamService : DreamService(), LifecycleOwner, SavedStateRegistryOwner {
  private val registry = LifecycleRegistry(this)
  private val savedState = SavedStateRegistryController.create(this)
  override val lifecycle: Lifecycle get() = registry
  override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

  private val data = mutableStateOf<ScreensaverData?>(null)

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
    setContentView(ComposeView(this).apply { setContent { Screensaver(data.value) } })
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

  override fun onDreamingStopped() {
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
    /** The running screensaver, so the adhan can end it before showing its card. Main thread only. */
    @Volatile var current: AdzanDreamService? = null
  }
}
