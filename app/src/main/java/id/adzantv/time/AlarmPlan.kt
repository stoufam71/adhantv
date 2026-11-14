package id.adzantv.time

import java.time.ZonedDateTime

/** The alarm each chain should hold; null means that chain's pending alarm is cancelled. */
data class AlarmPlan(val adhan: NextPrayer?, val reminder: NextReminder?) {
  companion object {
    /**
     * Each chain follows only its own switch. [days] (today, tomorrow) is read only when a chain is on,
     * since loading a schedule may hit the network.
     */
    fun of(
      adhanOn: Boolean,
      reminderOn: Boolean,
      now: ZonedDateTime,
      minutes: Int,
      prayers: Set<Prayer>,
      days: () -> Pair<PrayerTimes, PrayerTimes>,
    ): AlarmPlan {
      if (!adhanOn && !reminderOn) return AlarmPlan(null, null)
      val (today, tomorrow) = days()
      return AlarmPlan(
        adhan = if (adhanOn) PrayerClock.next(today, tomorrow, now) else null,
        reminder = if (reminderOn) ReminderClock.next(today, tomorrow, now, minutes, prayers) else null,
      )
    }
  }
}
