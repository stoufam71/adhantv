package id.adzantv.adhan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import id.adzantv.Adzan
import id.adzantv.dream.AdzanDreamService
import kotlin.concurrent.thread

class AlarmReceiver : BroadcastReceiver() {
  override fun onReceive(ctx: Context, intent: Intent) {
    val prayer = intent.getStringExtra(AdhanScheduler.EXTRA_PRAYER)
    val at = intent.getLongExtra(AdhanScheduler.EXTRA_AT, System.currentTimeMillis())
    val pending = goAsync()
    thread {
      try {
        // a TV in standby stays off (screensaver still counts as interactive)
        val screenOn = ctx.getSystemService(PowerManager::class.java).isInteractive
        if (prayer != null && screenOn && AdhanScheduler.shouldPlay(at, System.currentTimeMillis())) {
          // end our screensaver so the card is not hidden behind it
          Handler(Looper.getMainLooper()).post { AdzanDreamService.current?.finish() }
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
