package id.adzantv.adhan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import id.adzantv.Adzan
import kotlin.concurrent.thread

class AlarmReceiver : BroadcastReceiver() {
  override fun onReceive(ctx: Context, intent: Intent) {
    val prayer = intent.getStringExtra(AdhanScheduler.EXTRA_PRAYER)
    val at = intent.getLongExtra(AdhanScheduler.EXTRA_AT, System.currentTimeMillis())
    val pending = goAsync()
    thread {
      try {
        if (prayer != null && AdhanScheduler.shouldPlay(at, System.currentTimeMillis())) {
          // card first so it is on time; a failed start must not break the alarm chain below
          runCatching {
            ctx.startActivity(
              Intent(ctx, AdhanActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(AdhanScheduler.EXTRA_PRAYER, prayer)
                .putExtra(AdhanScheduler.EXTRA_AT, at),
            )
          }.onFailure { Log.e("AdzanTV", "adhan card start failed", it) }
        }
        Adzan.reschedule(ctx)
      } catch (e: Exception) {
        Log.e("AdzanTV", "alarm handling failed", e)
      } finally {
        pending.finish()
      }
    }
  }
}
