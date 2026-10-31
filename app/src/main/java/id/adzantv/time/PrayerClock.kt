package id.adzantv.time

import java.time.ZonedDateTime

data class NextPrayer(val prayer: Prayer, val at: ZonedDateTime)

object PrayerClock {
  /** First obligatory prayer strictly after [now]; a prayer whose minute is exactly now has already started. */
  fun next(today: PrayerTimes, tomorrow: PrayerTimes, now: ZonedDateTime): NextPrayer =
    sequenceOf(today, tomorrow)
      .flatMap { day -> day.times.entries.map { (p, t) -> NextPrayer(p, day.date.atTime(t).atZone(now.zone)) } }
      .filter { it.prayer.obligatory && it.at.isAfter(now) }
      .minBy { it.at }
}
