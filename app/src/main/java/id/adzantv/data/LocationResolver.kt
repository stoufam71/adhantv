package id.adzantv.data

import org.json.JSONObject

interface CityStore {
  var cityId: String?
  var cityName: String?
  var lat: Double?
  var lon: Double?
  var manual: Boolean
}

/** Guesses the Kemenag city from the public IP (ipwho.is). A manual choice wins unless [detect] is forced. */
class LocationResolver(
  private val store: CityStore,
  private val http: Http = httpGet,
  private val cities: () -> List<City>,
) {
  /** The newly stored city, or null when nothing changed (the caller opens the picker if no city is stored). */
  fun detect(force: Boolean = false): City? {
    val ip = runCatching { JSONObject(http("https://ipwho.is/")) }.getOrNull() ?: return null
    if (!ip.optBoolean("success")) return null
    // the offline calculator uses these even after a manual pick
    store.lat = ip.optDouble("latitude").takeUnless { it.isNaN() } ?: store.lat
    store.lon = ip.optDouble("longitude").takeUnless { it.isNaN() } ?: store.lon
    if (store.manual && !force) return null
    if (ip.optString("country_code") != "ID") return null
    val city = CityMatcher.match(ip.optString("city"), cities()) ?: return null
    store.cityId = city.id
    store.cityName = city.name
    store.manual = false
    return city
  }

  fun pick(city: City) {
    store.cityId = city.id
    store.cityName = city.name
    store.manual = true
  }
}
