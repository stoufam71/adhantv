package id.adzantv.adhan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import id.adzantv.Adzan
import kotlin.concurrent.thread

/** Boot, clock/zone changes and app updates all drop or invalidate the pending alarm. */
class BootReceiver : BroadcastReceiver() {
  override fun onReceive(ctx: Context, intent: Intent) {
    if (intent.action !in ACTIONS) return
    val pending = goAsync()
    thread {
      try {
        Adzan.reschedule(ctx)
      } catch (e: Exception) {
        Log.e("AdzanTV", "reschedule failed", e)
      } finally {
        pending.finish()
      }
    }
  }

  private companion object {
    val ACTIONS = setOf(
      Intent.ACTION_BOOT_COMPLETED,
      Intent.ACTION_TIME_CHANGED,
      Intent.ACTION_TIMEZONE_CHANGED,
      Intent.ACTION_MY_PACKAGE_REPLACED,
    )
  }
}
