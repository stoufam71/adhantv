package id.adzantv.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Plain GET returning the body; throws IOException on any failure. */
typealias Http = (String) -> String

val httpGet: Http = { url ->
  val conn = URL(url).openConnection() as HttpURLConnection
  try {
    conn.connectTimeout = 10_000
    conn.readTimeout = 10_000
    // ipwho.is answers 403 to Android's default "Dalvik/…" user agent
    conn.setRequestProperty("User-Agent", "AdzanTV/1.0")
    if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode} for $url")
    conn.inputStream.bufferedReader().use { it.readText() }
  } finally {
    conn.disconnect()
  }
}

/** Kemenag (Bimas Islam) prayer schedules via api.myquran.com. */
class MyQuranClient(private val http: Http = httpGet) {
  fun monthJson(cityId: String, year: Int, month: Int): String =
    http("$BASE/sholat/jadwal/$cityId/$year/${"%02d".format(month)}")

  fun citiesJson(): String = http("$BASE/sholat/kota/semua")

  private companion object {
    const val BASE = "https://api.myquran.com/v2"
  }
}
