package id.adzantv.time

import java.time.ZonedDateTime

data class NextReminder(val prayer: Prayer, val remindAt: ZonedDateTime, val prayerAt: ZonedDateTime)

/** The reminder shown a few minutes before a prayer; pure, like [PrayerClock]. */
object ReminderClock {
  /** The five prayers a reminder can be set for; Terbit has no adhan. */
  val ALL: Set<Prayer> = Prayer.entries.filter { it.obligatory }.toSet()

  private const val LATE_MS = 60_000L

  /**
   * First of [prayers] whose reminder, [minutes] before it, is strictly after [now]; a reminder whose
   * moment is exactly now has already been shown. Null when no prayer is selected.
   */
  fun next(today: PrayerTimes, tomorrow: PrayerTimes, now: ZonedDateTime, minutes: Int, prayers: Set<Prayer>): NextReminder? =
    sequenceOf(today, tomorrow)
      .flatMap { day -> day.times.entries.map { (p, t) -> p to day.date.atTime(t).atZone(now.zone) } }
      .filter { (p, _) -> p.obligatory && p in prayers }
      .map { (p, at) -> NextReminder(p, at.minusMinutes(minutes.toLong()), at) }
      .filter { it.remindAt.isAfter(now) }
      .minByOrNull { it.remindAt }

  /** A reminder delivered over a minute late (TV waking from standby), or after its prayer began, is only noise. */
  fun shouldShow(remindAt: Long, prayerAt: Long, now: Long) = now - remindAt <= LATE_MS && now < prayerAt

  /**
   * The selection after OK on [prayer], or on "Semua" when [prayer] is null. "Semua" always selects all;
   * the last selected prayer cannot be removed, so an active reminder never has nothing to remind of.
   */
  fun toggle(selected: Set<Prayer>, prayer: Prayer?): Set<Prayer> = when {
    prayer == null -> ALL
    prayer !in selected -> selected + prayer
    selected.size == 1 -> selected
    else -> selected - prayer
  }
}
