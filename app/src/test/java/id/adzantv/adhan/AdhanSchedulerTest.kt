package id.adzantv.adhan

import id.adzantv.time.Prayer
import id.adzantv.time.PrayerTimes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class AdhanSchedulerTest {
  private val oct1 = LocalDate.of(2026, 10, 1)
  private fun day(d: LocalDate) = PrayerTimes(
    d,
    mapOf(
      Prayer.SUBUH to LocalTime.of(4, 23), Prayer.TERBIT to LocalTime.of(5, 35),
      Prayer.DZUHUR to LocalTime.of(11, 46), Prayer.ASHAR to LocalTime.of(14, 51),
      Prayer.MAGHRIB to LocalTime.of(17, 50), Prayer.ISYA to LocalTime.of(18, 59),
    ),
  )

  /** One alarm slot, like AlarmManager with a single PendingIntent. */
  private class FakeAlarm : (Long, Prayer) -> Unit {
    var slot: Pair<Long, Prayer>? = null
    var sets = 0
    override fun invoke(at: Long, p: Prayer) { slot = at to p; sets++ }
  }

  private fun now(zone: String, h: Int) = ZonedDateTime.of(oct1, LocalTime.of(h, 0), ZoneId.of(zone))

  @Test fun noonSchedulesAsharToday() {
    val alarm = FakeAlarm()
    AdhanScheduler(alarm).scheduleNext(day(oct1), day(oct1.plusDays(1)), now("Asia/Jakarta", 12))
    val expected = ZonedDateTime.of(oct1, LocalTime.of(14, 51), ZoneId.of("Asia/Jakarta"))
    assertEquals(expected.toInstant().toEpochMilli() to Prayer.ASHAR, alarm.slot)
  }

  @Test fun schedulingTwiceKeepsOneAlarm() {
    val alarm = FakeAlarm()
    val s = AdhanScheduler(alarm)
    s.scheduleNext(day(oct1), day(oct1.plusDays(1)), now("Asia/Jakarta", 12))
    s.scheduleNext(day(oct1), day(oct1.plusDays(1)), now("Asia/Jakarta", 15))
    assertEquals(Prayer.MAGHRIB, alarm.slot!!.second)
  }

  @Test fun timeZoneChangeRecomputesInstant() {
    val wib = FakeAlarm().also { AdhanScheduler(it).scheduleNext(day(oct1), day(oct1.plusDays(1)), now("Asia/Jakarta", 12)) }
    val wita = FakeAlarm().also { AdhanScheduler(it).scheduleNext(day(oct1), day(oct1.plusDays(1)), now("Asia/Makassar", 12)) }
    assertEquals(3_600_000L, wib.slot!!.first - wita.slot!!.first)
  }

  @Test fun lateAlarmIsSkippedButOnTimeOnePlays() {
    val at = 1_000_000_000L
    assertTrue(AdhanScheduler.shouldPlay(at, at + 30_000))
    assertTrue(AdhanScheduler.shouldPlay(at, at + 5 * 60_000))
    assertFalse(AdhanScheduler.shouldPlay(at, at + 6 * 60_000))
  }
}
