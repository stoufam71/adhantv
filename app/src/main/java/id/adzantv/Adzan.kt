package id.adzantv

import android.content.Context
import id.adzantv.adhan.AdhanScheduler
import id.adzantv.data.FallbackCalculator
import id.adzantv.data.MyQuranClient
import id.adzantv.data.Prefs
import id.adzantv.data.ScheduleRepository
import id.adzantv.time.AlarmPlan
import id.adzantv.time.PrayerTimes
import java.time.LocalDate
import java.time.ZonedDateTime

/** App wiring. Everything here may hit the network: call it off the main thread. */
object Adzan {
  fun repository(ctx: Context): ScheduleRepository {
    val prefs = Prefs(ctx)
    return ScheduleRepository(ctx.filesDir, MyQuranClient()) { FallbackCalculator(prefs.lat, prefs.lon).forDate(it) }
  }

  fun day(ctx: Context, date: LocalDate): PrayerTimes = repository(ctx).forDate(Prefs(ctx).cityId, date)

  /**
   * Replaces the pending adhan alarm with one for the next prayer, and the pending reminder with the next
   * reminder; each chain is cleared when its own switch is off.
   */
  @Synchronized // settings key presses each start a reschedule; the last one in must win
  fun reschedule(ctx: Context) {
    val prefs = Prefs(ctx)
    val now = ZonedDateTime.now()
    val plan = AlarmPlan.of(prefs.adhanOn, prefs.reminderOn, now, prefs.reminderMinutes, prefs.reminderPrayers) {
      day(ctx, now.toLocalDate()) to day(ctx, now.toLocalDate().plusDays(1))
    }
    val adhan = plan.adhan
    if (adhan == null) {
      AdhanScheduler.cancel(ctx, AdhanScheduler.ADHAN_SLOT)
    } else {
      AdhanScheduler.set(ctx, adhan.at.toInstant().toEpochMilli(), adhan.prayer, AdhanScheduler.ADHAN_SLOT)
    }
    val reminder = plan.reminder
    if (reminder == null) {
      AdhanScheduler.cancel(ctx, AdhanScheduler.REMINDER_SLOT)
    } else {
      AdhanScheduler.set(
        ctx,
        reminder.remindAt.toInstant().toEpochMilli(),
        reminder.prayer,
        AdhanScheduler.REMINDER_SLOT,
        reminderFor = reminder.prayerAt.toInstant().toEpochMilli(),
      )
    }
  }
}
