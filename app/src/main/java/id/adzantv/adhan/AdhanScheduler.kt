package id.adzantv.adhan

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import id.adzantv.time.NextPrayer
import id.adzantv.time.Prayer
import id.adzantv.time.PrayerClock
import id.adzantv.time.PrayerTimes
import java.time.ZonedDateTime

/** Keeps exactly one alarm pending: the next obligatory prayer. */
class AdhanScheduler(private val setAlarm: (atMillis: Long, prayer: Prayer) -> Unit) {
  fun scheduleNext(today: PrayerTimes, tomorrow: PrayerTimes, now: ZonedDateTime): NextPrayer =
    PrayerClock.next(today, tomorrow, now).also { setAlarm(it.at.toInstant().toEpochMilli(), it.prayer) }

  companion object {
    const val EXTRA_PRAYER = "prayer"
    const val EXTRA_AT = "at"
    private const val LATE_MS = 5 * 60_000L

    /** An adhan long after its time (e.g. delivered when the TV wakes from standby) is wrong, so it is skipped. */
    fun shouldPlay(scheduledAt: Long, now: Long) = now - scheduledAt <= LATE_MS

    fun forDevice(ctx: Context) = AdhanScheduler { at, prayer -> set(ctx, at, prayer) }

    /** [slot] 0 is the real chain; the settings test uses 1 so a reschedule never cancels it. */
    fun set(ctx: Context, at: Long, prayer: Prayer, slot: Int = 0) {
      val am = ctx.getSystemService(AlarmManager::class.java)
      val intent = Intent(ctx, AlarmReceiver::class.java)
        .putExtra(EXTRA_PRAYER, prayer.name)
        .putExtra(EXTRA_AT, at)
      val pi = pending(ctx, slot, intent, PendingIntent.FLAG_UPDATE_CURRENT)!! // never null without FLAG_NO_CREATE
      if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
      } else {
        am.setAlarmClock(AlarmManager.AlarmClockInfo(at, null), pi)
      }
    }

    /** Drops the pending alarm in [slot], if any. */
    fun cancel(ctx: Context, slot: Int = 0) {
      pending(ctx, slot, Intent(ctx, AlarmReceiver::class.java), PendingIntent.FLAG_NO_CREATE)
        ?.let { ctx.getSystemService(AlarmManager::class.java).cancel(it); it.cancel() }
    }

    // one request code per slot, so a new alarm always replaces the previous one; extras do not affect matching
    private fun pending(ctx: Context, slot: Int, intent: Intent, flags: Int): PendingIntent? =
      PendingIntent.getBroadcast(ctx, slot, intent, flags or PendingIntent.FLAG_IMMUTABLE)
  }
}
