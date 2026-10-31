package id.adzantv.adhan

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import id.adzantv.R

/** Plays the bundled adhan with transient audio focus, which pauses other media until [stop]. */
class AdhanPlayer(private val ctx: Context, private val onDone: () -> Unit) {
  private var player: MediaPlayer? = null
  private var focus: AudioFocusRequest? = null
  private val handler = Handler(Looper.getMainLooper())
  private val audio get() = ctx.getSystemService(AudioManager::class.java)

  fun start() {
    val attrs = AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_MEDIA)
      .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
      .build()
    focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attrs).build()
    audio.requestAudioFocus(focus!!)
    player = try {
      MediaPlayer.create(ctx, voice(), attrs, audio.generateAudioSessionId())
    } catch (e: Exception) {
      Log.e("AdzanTV", "adhan audio failed", e)
      null
    }
    val p = player
    if (p == null) {
      // keep the card visible for a minute even without sound
      handler.postDelayed(onDone, 60_000)
      return
    }
    p.setOnCompletionListener { onDone() }
    p.start()
  }

  /**
   * The Makkah adhan when tools/fetch-adhan.sh has put it in res/raw (it is unlicensed, so never committed),
   * else the bundled CC0 one. Looked up by name because the resource may not exist at build time.
   */
  @SuppressLint("DiscouragedApi")
  private fun voice(): Int =
    ctx.resources.getIdentifier("adhan_makkah", "raw", ctx.packageName).takeIf { it != 0 } ?: R.raw.adhan

  /** Milliseconds played and total, for the card's progress line; null without sound. Main thread. */
  fun progress(): Pair<Int, Int>? = player?.let { p -> runCatching { p.currentPosition to p.duration }.getOrNull() }

  fun stop() {
    handler.removeCallbacksAndMessages(null)
    player?.release()
    player = null
    focus?.let { audio.abandonAudioFocusRequest(it) }
    focus = null
  }
}
