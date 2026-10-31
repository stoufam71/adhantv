package id.adzantv.adhan

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import id.adzantv.R
import id.adzantv.data.Prefs
import id.adzantv.time.Prayer
import id.adzantv.ui.AdhanCard
import java.time.Instant
import java.time.ZoneId

/** Full-screen call to prayer over whatever is playing; Back stops it. */
class AdhanActivity : ComponentActivity() {
  private var player: MediaPlayer? = null
  private var focus: AudioFocusRequest? = null
  private val handler = Handler(Looper.getMainLooper())

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setShowWhenLocked(true)
    setTurnScreenOn(true)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    val prayer = intent.getStringExtra(AdhanScheduler.EXTRA_PRAYER)?.let { runCatching { Prayer.valueOf(it) }.getOrNull() }
    val at = Instant.ofEpochMilli(intent.getLongExtra(AdhanScheduler.EXTRA_AT, System.currentTimeMillis()))
      .atZone(ZoneId.systemDefault()).toLocalTime()
    setContent { AdhanCard(prayer?.label ?: "Adzan", at, Prefs(this).cityName) }
    play()
  }

  private fun play() {
    val attrs = AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_MEDIA)
      .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
      .build()
    // transient focus pauses other media; it resumes when we abandon focus
    focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attrs).build()
    getSystemService(AudioManager::class.java).requestAudioFocus(focus!!)
    player = try {
      MediaPlayer.create(this, R.raw.adhan, attrs, getSystemService(AudioManager::class.java).generateAudioSessionId())
    } catch (e: Exception) {
      Log.e("AdzanTV", "adhan audio failed", e)
      null
    }
    val p = player
    if (p == null) {
      handler.postDelayed({ finish() }, 60_000)
      return
    }
    p.setOnCompletionListener { finish() }
    p.start()
  }

  override fun onStop() {
    super.onStop()
    // leaving the card (Back, Home, another app) ends the call
    finish()
  }

  override fun onDestroy() {
    handler.removeCallbacksAndMessages(null)
    player?.release()
    player = null
    focus?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
    super.onDestroy()
  }
}
