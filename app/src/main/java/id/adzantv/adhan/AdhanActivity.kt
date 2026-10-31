package id.adzantv.adhan

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import id.adzantv.data.Prefs
import id.adzantv.time.Prayer
import id.adzantv.ui.AdhanCard
import java.time.Instant
import java.time.ZoneId

/** Full-screen call to prayer over whatever is playing; Back or the remote's OK stops it. */
class AdhanActivity : ComponentActivity() {
  private val player by lazy { AdhanPlayer(this) { finish() } }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    val prayer = intent.getStringExtra(AdhanScheduler.EXTRA_PRAYER)?.let { runCatching { Prayer.valueOf(it) }.getOrNull() }
    val at = Instant.ofEpochMilli(intent.getLongExtra(AdhanScheduler.EXTRA_AT, System.currentTimeMillis()))
      .atZone(ZoneId.systemDefault()).toLocalTime()
    setContent { AdhanCard(prayer?.label ?: "Adzan", at, Prefs(this).cityName) }
    player.start()
  }

  /** The remote's centre button is the reflex stop press; make it work alongside Back. */
  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
      finish()
      return true
    }
    return super.onKeyDown(keyCode, event)
  }

  override fun onStop() {
    super.onStop()
    // leaving the card (Back, Home, another app) ends the call
    finish()
  }

  override fun onDestroy() {
    player.stop()
    super.onDestroy()
  }
}
