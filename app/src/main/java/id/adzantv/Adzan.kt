package id.adzantv

import android.content.Context
import id.adzantv.adhan.AdhanScheduler
import id.adzantv.data.FallbackCalculator
import id.adzantv.data.MyQuranClient
import id.adzantv.data.Prefs
import id.adzantv.data.ScheduleRepository
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

  /** Replaces the single pending adhan alarm with one for the next prayer, or clears it when adhan is off. */
  fun reschedule(ctx: Context) {
    if (!Prefs(ctx).adhanOn) {
      AdhanScheduler.cancel(ctx)
      return
    }
    val now = ZonedDateTime.now()
    val today = now.toLocalDate()
    AdhanScheduler.forDevice(ctx).scheduleNext(day(ctx, today), day(ctx, today.plusDays(1)), now)
  }
}
