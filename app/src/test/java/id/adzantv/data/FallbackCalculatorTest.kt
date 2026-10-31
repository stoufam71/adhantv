package id.adzantv.data

import id.adzantv.time.Prayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

class FallbackCalculatorTest {
  private val zone = ZoneId.of("Asia/Jakarta")
  private val official = ScheduleRepository(
    kotlin.io.path.createTempDirectory().toFile(),
    MyQuranClient { javaClass.classLoader!!.getResource("jadwal-1301-2026-10.json")!!.readText() },
  ) { error("fixture must parse") }

  private fun assertWithin2Min(date: LocalDate) {
    val kemenag = official.forDate("1301", date)
    val calc = FallbackCalculator(-6.2146, 106.8451) { zone }.forDate(date)
    assertTrue(calc.estimated)
    for (p in Prayer.entries.filter { it.obligatory }) {
      val diff = Duration.between(kemenag.times[p], calc.times[p]).abs().toMinutes()
      assertTrue("$p on $date: kemenag ${kemenag.times[p]} calc ${calc.times[p]}", diff <= 2)
    }
  }

  @Test fun jakartaOct1WithinTwoMinutes() = assertWithin2Min(LocalDate.of(2026, 10, 1))

  @Test fun jakartaOct31WithinTwoMinutes() = assertWithin2Min(LocalDate.of(2026, 10, 31))

  @Test fun noStoredCoordinatesStillGivesSixTimes() { // AE3
    val day = FallbackCalculator(null, null) { zone }.forDate(LocalDate.of(2026, 10, 1))
    assertEquals(6, day.times.size)
  }
}
