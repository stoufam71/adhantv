package id.adzantv.time

import java.time.LocalDate
import java.time.LocalTime

/** The six daily times Kemenag publishes that the app shows; Terbit is shown but never called. */
enum class Prayer(val label: String, val key: String, val obligatory: Boolean) {
  SUBUH("Subuh", "subuh", true),
  TERBIT("Terbit", "terbit", false),
  DZUHUR("Dzuhur", "dzuhur", true),
  ASHAR("Ashar", "ashar", true),
  MAGHRIB("Maghrib", "maghrib", true),
  ISYA("Isya", "isya", true),
}

/** One day's times in local (device) time. [estimated] is true when computed offline instead of from Kemenag. */
data class PrayerTimes(
  val date: LocalDate,
  val times: Map<Prayer, LocalTime>,
  val estimated: Boolean = false,
)
