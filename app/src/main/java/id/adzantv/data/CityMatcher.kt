package id.adzantv.data

import org.json.JSONObject
import java.io.File

data class City(val id: String, val name: String)

object CityMatcher {
  fun parse(json: String): List<City> {
    val arr = JSONObject(json).getJSONArray("data")
    return (0 until arr.length()).map { arr.getJSONObject(it) }.map { City(it.getString("id"), it.getString("lokasi")) }
  }

  /**
   * The one Kemenag city whose base name equals [ipCity], or null.
   * shortcut: name equality only, no coordinates; a city the IP service spells differently falls to the picker.
   * Upgrade by bundling kabupaten/kota centroids if users hit the picker often.
   */
  fun match(ipCity: String, cities: List<City>): City? {
    val input = ipCity.trim().uppercase()
    val wantsKab = KAB_PREFIXES.any { input.startsWith(it) }
    val candidates = cities.filter { base(it.name) == base(input) }
    val preferred = when {
      wantsKab -> candidates.filter { it.name.startsWith("KAB. ") }
      candidates.any { it.name.startsWith("KOTA ") } -> candidates.filter { it.name.startsWith("KOTA ") }
      else -> candidates
    }
    return preferred.singleOrNull()
  }

  private val KAB_PREFIXES = listOf("KABUPATEN ", "KAB. ", "KAB ")

  private fun base(name: String): String {
    val n = name.trim().uppercase()
    val prefix = (KAB_PREFIXES + "KOTA ").firstOrNull { n.startsWith(it) }
    return (if (prefix != null) n.removePrefix(prefix) else n).trim()
  }
}

/** The Kemenag city list, cached as the raw `kota/semua` response so the picker works offline. Empty if unavailable. */
fun loadCities(cacheDir: File, client: MyQuranClient): List<City> {
  val file = File(cacheDir, "kota-semua.json")
  if (file.exists()) runCatching { return CityMatcher.parse(file.readText()) }
  return runCatching {
    val body = client.citiesJson()
    CityMatcher.parse(body).also { file.writeText(body) }
  }.getOrDefault(emptyList())
}
