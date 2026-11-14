package id.adzantv.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmPlanTest {
  private val zone = ZoneId.of("Asia/Jakarta")
  private val day1 = LocalDate.of(2026, 11, 14)
  private val now = ZonedDateTime.of(day1, LocalTime.of(14, 0), zone)

  private fun day(date: LocalDate) = PrayerTimes(
    date,
    mapOf(
      Prayer.SUBUH to LocalTime.of(4, 3),
      Prayer.TERBIT to LocalTime.of(5, 20),
      Prayer.DZUHUR to LocalTime.of(11, 38),
      Prayer.ASHAR to LocalTime.of(15, 0),
      Prayer.MAGHRIB to LocalTime.of(17, 50),
      Prayer.ISYA to LocalTime.of(19, 3),
    ),
  )

  private val ashar = ZonedDateTime.of(day1, LocalTime.of(15, 0), zone)
  private val adhan = NextPrayer(Prayer.ASHAR, ashar)
  private val reminder = NextReminder(Prayer.ASHAR, ashar.minusMinutes(5), ashar)

  private fun plan(adhanOn: Boolean, reminderOn: Boolean, prayers: Set<Prayer> = ReminderClock.ALL) =
    AlarmPlan.of(adhanOn, reminderOn, now, 5, prayers) { day(day1) to day(day1.plusDays(1)) }

  @Test fun bothOnArmsBothChains() {
    assertEquals(AlarmPlan(adhan, reminder), plan(adhanOn = true, reminderOn = true))
  }

  @Test fun adhanOffKeepsTheReminder() {
    assertEquals(AlarmPlan(null, reminder), plan(adhanOn = false, reminderOn = true))
  }

  @Test fun reminderOffKeepsTheAdhan() {
    assertEquals(AlarmPlan(adhan, null), plan(adhanOn = true, reminderOn = false))
  }

  @Test fun bothOffCancelsBothWithoutLoadingASchedule() {
    var loaded = false
    val plan = AlarmPlan.of(false, false, now, 5, ReminderClock.ALL) { loaded = true; day(day1) to day(day1.plusDays(1)) }
    assertEquals(AlarmPlan(null, null), plan)
    assertFalse(loaded)
  }

  @Test fun noPrayerSelectedCancelsOnlyTheReminder() {
    val plan = plan(adhanOn = true, reminderOn = true, prayers = emptySet())
    assertEquals(adhan, plan.adhan)
    assertNull(plan.reminder)
  }
}
