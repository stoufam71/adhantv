package id.adzantv.data

import id.adzantv.time.Prayer
import id.adzantv.time.PrayerTimes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime

class ScheduleRepositoryTest {
  @get:Rule val tmp = TemporaryFolder()

  private fun fixture(name: String) = javaClass.classLoader!!.getResource(name)!!.readText()

  private val fallbackDay = { date: LocalDate ->
    PrayerTimes(date, Prayer.entries.associateWith { LocalTime.NOON }, estimated = true)
  }

  private class FakeHttp(val responses: Map<String, String>) : (String) -> String {
    var calls = 0
    override fun invoke(url: String): String {
      calls++
      return responses.entries.firstOrNull { url.endsWith(it.key) }?.value ?: throw IOException("offline")
    }
  }

  private fun repo(http: FakeHttp) = ScheduleRepository(tmp.root, MyQuranClient(http), fallbackDay)

  @Test fun parsesKemenagFixture() {
    val r = repo(FakeHttp(mapOf("/1301/2026/10" to fixture("jadwal-1301-2026-10.json"))))
    val day = r.forDate("1301", LocalDate.of(2026, 10, 1))
    assertFalse(day.estimated)
    assertEquals(LocalTime.of(4, 23), day.times[Prayer.SUBUH])
    assertEquals(LocalTime.of(18, 59), day.times[Prayer.ISYA])
    assertEquals(6, day.times.size)
  }

  @Test fun secondReadOfSameMonthUsesCache() {
    val http = FakeHttp(mapOf("/1301/2026/10" to fixture("jadwal-1301-2026-10.json")))
    val r = repo(http)
    r.forDate("1301", LocalDate.of(2026, 10, 1))
    r.forDate("1301", LocalDate.of(2026, 10, 31))
    assertEquals(1, http.calls)
  }

  @Test fun monthBoundaryFetchesNextMonthOnDemand() {
    val http = FakeHttp(
      mapOf(
        "/1301/2026/10" to fixture("jadwal-1301-2026-10.json"),
        "/1301/2026/11" to fixture("jadwal-1301-2026-11.json"),
      ),
    )
    val nov1 = repo(http).forDate("1301", LocalDate.of(2026, 11, 1))
    assertFalse(nov1.estimated)
    assertEquals(LocalDate.of(2026, 11, 1), nov1.date)
  }

  @Test fun offlineWithoutCacheFallsBack() {
    val day = repo(FakeHttp(emptyMap())).forDate("1301", LocalDate.of(2026, 10, 1))
    assertTrue(day.estimated)
  }

  @Test fun noCityFallsBack() {
    assertTrue(repo(FakeHttp(emptyMap())).forDate(null, LocalDate.of(2026, 10, 1)).estimated)
  }

  @Test fun malformedResponseDoesNotOverwriteGoodCache() {
    val good = FakeHttp(mapOf("/1301/2026/10" to fixture("jadwal-1301-2026-10.json")))
    repo(good).forDate("1301", LocalDate.of(2026, 10, 1))
    // same cache dir, server now returns garbage: the cached month still answers
    val bad = repo(FakeHttp(mapOf("/1301/2026/10" to "{not json")))
    assertFalse(bad.forDate("1301", LocalDate.of(2026, 10, 2)).estimated)
  }

  @Test fun malformedResponseWithNoCacheFallsBack() {
    val r = repo(FakeHttp(mapOf("/1301/2026/10" to "{\"status\":false}")))
    assertTrue(r.forDate("1301", LocalDate.of(2026, 10, 1)).estimated)
    assertFalse(tmp.root.resolve("1301-2026-10.json").exists())
  }
}
