package id.adzantv.data

import android.content.Context
import androidx.core.content.edit

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
  /** Id of the chosen colour palette; see id.adzantv.ui.Palettes. */
  var palette: String?
    get() = sp.getString("palette", null)
    set(v) = sp.edit { putString("palette", v) }
  override var manual: Boolean
    get() = sp.getBoolean("manual", false)
    set(v) = sp.edit { putBoolean("manual", v) }
}
