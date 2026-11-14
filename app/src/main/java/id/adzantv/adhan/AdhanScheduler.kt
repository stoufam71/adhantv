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

/**
 * Picks the next obligatory prayer for one alarm. The device holds up to three, one per slot:
 * [ADHAN_SLOT] the adhan chain, [TEST_SLOT] the settings test, [REMINDER_SLOT] the reminder chain.
 */
class AdhanScheduler(private val setAlarm: (atMillis: Long, prayer: Prayer) -> Unit) {
  fun scheduleNext(today: PrayerTimes, tomorrow: PrayerTimes, now: ZonedDateTime): NextPrayer =
    PrayerClock.next(today, tomorrow, now).also { setAlarm(it.at.toInstant().toEpochMilli(), it.prayer) }

  companion object {
    const val EXTRA_PRAYER = "prayer"
    const val EXTRA_AT = "at"

    /** Present on a reminder alarm: the prayer's own time, so a late reminder can be dropped. */
    const val EXTRA_REMINDER_FOR = "reminderFor"

    /** The adhan chain: always the next prayer while "Adzan otomatis" is on. */
    const val ADHAN_SLOT = 0

    /** The settings test alarm, apart from the chains so a reschedule never cancels it. */
    const val TEST_SLOT = 1

    /** The reminder chain's own slot, so it lives and dies apart from the adhan chain. */
    const val REMINDER_SLOT = 2
    private const val LATE_MS = 5 * 60_000L

    /** An adhan long after its time (e.g. delivered when the TV wakes from standby) is wrong, so it is skipped. */
    fun shouldPlay(scheduledAt: Long, now: Long) = now - scheduledAt <= LATE_MS

    /** Arms [slot] (see the class doc); [reminderFor] marks a reminder and carries its prayer's time. */
    fun set(ctx: Context, at: Long, prayer: Prayer, slot: Int = ADHAN_SLOT, reminderFor: Long? = null) {
      val am = ctx.getSystemService(AlarmManager::class.java)
      val intent = Intent(ctx, AlarmReceiver::class.java)
        .putExtra(EXTRA_PRAYER, prayer.name)
        .putExtra(EXTRA_AT, at)
      if (reminderFor != null) intent.putExtra(EXTRA_REMINDER_FOR, reminderFor)
      val pi = pending(ctx, slot, intent, PendingIntent.FLAG_UPDATE_CURRENT)!! // never null without FLAG_NO_CREATE
      if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
      } else {
        am.setAlarmClock(AlarmManager.AlarmClockInfo(at, null), pi)
      }
    }

    /** Drops the pending alarm in [slot], if any. */
    fun cancel(ctx: Context, slot: Int = ADHAN_SLOT) {
      pending(ctx, slot, Intent(ctx, AlarmReceiver::class.java), PendingIntent.FLAG_NO_CREATE)
        ?.let { ctx.getSystemService(AlarmManager::class.java).cancel(it); it.cancel() }
    }

    // one request code per slot, so a new alarm always replaces the previous one; extras do not affect matching
    private fun pending(ctx: Context, slot: Int, intent: Intent, flags: Int): PendingIntent? =
      PendingIntent.getBroadcast(ctx, slot, intent, flags or PendingIntent.FLAG_IMMUTABLE)
  }
}
