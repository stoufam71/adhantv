package id.adzantv.data

import android.content.Context
import androidx.core.content.edit
import id.adzantv.time.Prayer
import id.adzantv.time.ReminderClock

class Prefs(context: Context) : CityStore {
  private val sp = context.getSharedPreferences("adzan", Context.MODE_PRIVATE)

  override var cityId: String?
    get() = sp.getString("cityId", null)
    set(v) = sp.edit { putString("cityId", v) }
  override var cityName: String?
    get() = sp.getString("cityName", null)
    set(v) = sp.edit { putString("cityName", v) }
  override var lat: Double?
    get() = sp.getString("lat", null)?.toDouble()
    set(v) = sp.edit { putString("lat", v?.toString()) }
  override var lon: Double?
    get() = sp.getString("lon", null)?.toDouble()
    set(v) = sp.edit { putString("lon", v?.toString()) }
  /** Off: no adhan alarm is kept; the schedule and screensaver still show. */
  var adhanOn: Boolean
    get() = sp.getBoolean("adhanOn", true)
    set(v) = sp.edit { putBoolean("adhanOn", v) }

  /** A silent card shortly before each chosen prayer; independent of [adhanOn]. */
  var reminderOn: Boolean
    get() = sp.getBoolean("reminderOn", true)
    set(v) = sp.edit { putBoolean("reminderOn", v) }

  /** Minutes before the prayer, always 1 to 10. */
  var reminderMinutes: Int
    get() = sp.getInt("reminderMinutes", 5).coerceIn(1, 10)
    set(v) = sp.edit { putInt("reminderMinutes", v.coerceIn(1, 10)) }

  /** Prayers that get a reminder. Stored only when it is not all five, so the default stays "all". */
  var reminderPrayers: Set<Prayer>
    get() = sp.getStringSet("reminderPrayers", null)
      ?.mapNotNull { n -> ReminderClock.ALL.firstOrNull { it.name == n } }
      ?.toSet()
      ?.ifEmpty { null }
      ?: ReminderClock.ALL
    set(v) = sp.edit {
      if (v.containsAll(ReminderClock.ALL)) remove("reminderPrayers") else putStringSet("reminderPrayers", v.map { it.name }.toSet())
    }

  /** Id of the chosen colour palette; see id.adzantv.ui.Palettes. */
  var palette: String?
    get() = sp.getString("palette", null)
    set(v) = sp.edit { putString("palette", v) }
  override var manual: Boolean
    get() = sp.getBoolean("manual", false)
    set(v) = sp.edit { putBoolean("manual", v) }
}
