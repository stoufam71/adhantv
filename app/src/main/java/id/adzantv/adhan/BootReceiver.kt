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
}
