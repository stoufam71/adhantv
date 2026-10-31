package id.adzantv.data

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.data.DateComponents
import id.adzantv.time.Prayer
import id.adzantv.time.PrayerTimes
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import com.batoulapps.adhan.PrayerTimes as AdhanTimes

/**
 * Offline Kemenag-style times: Fajr 20°, Isha 18°, Shafi'i Asr, plus ihtiyat minutes.
 * shortcut: Jakarta is used before any IP lookup has succeeded; upgrade if an offline first boot ever matters.
 */
class FallbackCalculator(lat: Double?, lon: Double?, private val zone: () -> ZoneId = ZoneId::systemDefault) {
  private val coords = Coordinates(lat ?: -6.2146, lon ?: 106.8451)

  private val params = CalculationMethod.OTHER.parameters.apply {
    fajrAngle = 20.0
    ishaAngle = 18.0
    madhab = Madhab.SHAFI
    adjustments.fajr = 2
    adjustments.sunrise = -2
    adjustments.dhuhr = 2
    adjustments.asr = 2
    adjustments.maghrib = 2
    adjustments.isha = 2
  }

  fun forDate(date: LocalDate): PrayerTimes {
    val t = AdhanTimes(coords, DateComponents(date.year, date.monthValue, date.dayOfMonth), params)
    fun local(d: Date) = d.toInstant().atZone(zone()).toLocalTime()
    return PrayerTimes(
      date,
      mapOf(
        Prayer.SUBUH to local(t.fajr),
        Prayer.TERBIT to local(t.sunrise),
        Prayer.DZUHUR to local(t.dhuhr),
        Prayer.ASHAR to local(t.asr),
        Prayer.MAGHRIB to local(t.maghrib),
        Prayer.ISYA to local(t.isha),
      ),
      estimated = true,
    )
  }
}
