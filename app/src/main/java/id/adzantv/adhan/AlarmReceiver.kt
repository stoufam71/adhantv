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
import id.adzantv.data.Prefs
import id.adzantv.time.Prayer
import id.adzantv.time.ReminderClock

class AlarmReceiver : BroadcastReceiver() {
  override fun onReceive(ctx: Context, intent: Intent) {
    val prayer = intent.getStringExtra(AdhanScheduler.EXTRA_PRAYER)
    val at = intent.getLongExtra(AdhanScheduler.EXTRA_AT, System.currentTimeMillis())
    val pending = goAsync()
    thread {
      try {
        // a TV in standby stays off (screensaver still counts as interactive)
        val screenOn = ctx.getSystemService(PowerManager::class.java).isInteractive
        Log.i("AdzanTV", "alarm prayer=$prayer lateMs=${System.currentTimeMillis() - at} screenOn=$screenOn")
        val reminderFor = intent.getLongExtra(AdhanScheduler.EXTRA_REMINDER_FOR, 0)
        if (reminderFor > 0) {
          val p = prayer?.let { runCatching { Prayer.valueOf(it) }.getOrNull() }
          if (p != null && screenOn && ReminderClock.shouldShow(at, reminderFor, System.currentTimeMillis())) {
            val minutes = Prefs(ctx).reminderMinutes
            Handler(Looper.getMainLooper()).post {
              // our own screensaver already counts down to the prayer, and a window above a dream is device-dependent
              if (AdzanDreamService.current == null) ReminderOverlay.show(ctx, p, reminderFor, minutes)
            }
          }
        } else if (prayer != null && screenOn && AdhanScheduler.shouldPlay(at, System.currentTimeMillis())) {
          // card first so it is on time; a failed start must not break the alarm chain below
          Handler(Looper.getMainLooper()).post {
            val dream = AdzanDreamService.current
            // ending a screensaver with no user input sends Google TV to sleep, killing any card;
            // so our own screensaver shows the call itself
            if (dream != null) {
              dream.showAdhan(prayer, at)
            } else {
              runCatching {
                ctx.startActivity(
                  Intent(ctx, AdhanActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(AdhanScheduler.EXTRA_PRAYER, prayer)
                    .putExtra(AdhanScheduler.EXTRA_AT, at),
                )
              }.onFailure { Log.e("AdzanTV", "adhan card start failed", it) }
            }
          }
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
