package id.adzantv.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderClockTest {
  private val zone = ZoneId.of("Asia/Jakarta")
  private val day1 = LocalDate.of(2026, 11, 14)

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

  private fun at(h: Int, m: Int, s: Int = 0, date: LocalDate = day1) = ZonedDateTime.of(date, LocalTime.of(h, m, s), zone)

  private fun next(now: ZonedDateTime, minutes: Int = 5, prayers: Set<Prayer> = ReminderClock.ALL) =
    ReminderClock.next(day(day1), day(day1.plusDays(1)), now, minutes, prayers)

  @Test fun fiveMinutesBeforeAshar() {
    assertEquals(NextReminder(Prayer.ASHAR, at(14, 55), at(15, 0)), next(at(14, 0)))
  }

  @Test fun insideTheLeadWindowSkipsToTheNextPrayer() {
    assertEquals(Prayer.MAGHRIB, next(at(14, 57))?.prayer)
  }

  @Test fun exactlyAtTheReminderCountsAsShown() {
    assertEquals(Prayer.MAGHRIB, next(at(14, 55))?.prayer)
  }

  @Test fun afterIsyaGivesTomorrowSubuh() {
    assertEquals(
      NextReminder(Prayer.SUBUH, at(3, 58, date = day1.plusDays(1)), at(4, 3, date = day1.plusDays(1))),
      next(at(21, 0)),
    )
  }

  @Test fun onlySelectedPrayersRemind() {
    val some = setOf(Prayer.SUBUH, Prayer.MAGHRIB, Prayer.ISYA)
    assertEquals(Prayer.MAGHRIB, next(at(10, 0), prayers = some)?.prayer)
  }

  @Test fun terbitNeverReminds() {
    assertEquals(Prayer.DZUHUR, next(at(4, 30), prayers = ReminderClock.ALL + Prayer.TERBIT)?.prayer)
  }

  @Test fun leadIsExactlyTheChosenMinutes() {
    assertEquals(at(14, 59), next(at(14, 0), minutes = 1)?.remindAt)
    assertEquals(at(14, 50), next(at(14, 0), minutes = 10)?.remindAt)
  }

  @Test fun nothingSelectedGivesNothing() {
    assertNull(next(at(10, 0), prayers = emptySet()))
  }

  @Test fun lateOrPassedRemindersAreSkipped() {
    val remind = at(14, 55).toInstant().toEpochMilli()
    val prayer = at(15, 0).toInstant().toEpochMilli()
    assertTrue(ReminderClock.shouldShow(remind, prayer, remind + 30_000))
    assertFalse(ReminderClock.shouldShow(remind, prayer, remind + 61_000))
    assertFalse(ReminderClock.shouldShow(remind, prayer, prayer))
  }

  @Test fun togglingKeepsAtLeastOne() {
    val all = ReminderClock.ALL
    assertEquals(all - Prayer.ASHAR, ReminderClock.toggle(all, Prayer.ASHAR))
    assertEquals(all, ReminderClock.toggle(all - Prayer.ASHAR, Prayer.ASHAR))
    assertEquals(setOf(Prayer.ISYA), ReminderClock.toggle(setOf(Prayer.ISYA), Prayer.ISYA))
    assertEquals(all, ReminderClock.toggle(setOf(Prayer.ISYA), null))
    assertEquals(all, ReminderClock.toggle(all, null))
  }
}
