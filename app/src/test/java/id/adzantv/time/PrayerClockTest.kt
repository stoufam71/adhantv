package id.adzantv.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class PrayerClockTest {
  private val zone = ZoneId.of("Asia/Jakarta")
  private val oct1 = LocalDate.of(2026, 10, 1)

  private fun day(date: LocalDate) = PrayerTimes(
    date,
    mapOf(
      Prayer.SUBUH to LocalTime.of(4, 23),
      Prayer.TERBIT to LocalTime.of(5, 35),
      Prayer.DZUHUR to LocalTime.of(11, 46),
      Prayer.ASHAR to LocalTime.of(14, 51),
      Prayer.MAGHRIB to LocalTime.of(17, 50),
      Prayer.ISYA to LocalTime.of(18, 59),
    ),
  )

  private fun at(h: Int, m: Int, s: Int = 0) = ZonedDateTime.of(oct1, LocalTime.of(h, m, s), zone)

  private fun next(now: ZonedDateTime) = PrayerClock.next(day(oct1), day(oct1.plusDays(1)), now)

  @Test fun noonGivesAshar() {
    assertEquals(NextPrayer(Prayer.ASHAR, at(14, 51)), next(at(12, 0)))
  }

  @Test fun afterIsyaGivesTomorrowSubuh() {
    assertEquals(
      NextPrayer(Prayer.SUBUH, ZonedDateTime.of(oct1.plusDays(1), LocalTime.of(4, 23), zone)),
      next(at(21, 0)),
    )
  }

  @Test fun exactPrayerMinuteMovesToFollowingPrayer() {
    assertEquals(Prayer.ASHAR, next(at(11, 46, 0)).prayer)
  }

  @Test fun terbitIsNeverNext() {
    assertEquals(Prayer.DZUHUR, next(at(5, 0)).prayer)
  }
}
