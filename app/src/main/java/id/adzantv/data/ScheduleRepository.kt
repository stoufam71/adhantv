package id.adzantv.data

import id.adzantv.time.Prayer
import id.adzantv.time.PrayerTimes
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

/**
 * Kemenag times for a city and day: month cache first, then the API, then [fallback].
 * The cache is the raw myQuran month response in `<cityId>-<yyyy>-<mm>.json`.
 */
class ScheduleRepository(
  private val cacheDir: File,
  private val client: MyQuranClient,
  private val fallback: (LocalDate) -> PrayerTimes,
) {
  fun forDate(cityId: String?, date: LocalDate): PrayerTimes {
    if (cityId == null) return fallback(date)
    val file = File(cacheDir, "$cityId-${date.year}-${"%02d".format(date.monthValue)}.json")
    val cached = if (file.exists()) parseDay(file.readText(), date) else null
    if (cached != null) return cached
    return try {
      val body = client.monthJson(cityId, date.year, date.monthValue)
      // parse before writing so a bad response never replaces a good cache
      val day = parseDay(body, date) ?: return fallback(date)
      file.writeText(body)
      day
    } catch (e: Exception) {
      fallback(date)
    }
  }

  private fun parseDay(json: String, date: LocalDate): PrayerTimes? = try {
    val days = JSONObject(json).getJSONObject("data").getJSONArray("jadwal")
    val iso = date.toString()
    (0 until days.length()).map { days.getJSONObject(it) }.firstOrNull { it.getString("date") == iso }
      ?.let { d -> PrayerTimes(date, Prayer.entries.associateWith { LocalTime.parse(d.getString(it.key)) }) }
  } catch (e: Exception) {
    null
  }
}
