package id.adzantv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class LocationResolverTest {
  private val cities = CityMatcher.parse(javaClass.classLoader!!.getResource("kota-semua.json")!!.readText())

  private class MemoryStore : CityStore {
    override var cityId: String? = null
    override var cityName: String? = null
    override var lat: Double? = null
    override var lon: Double? = null
    override var manual: Boolean = false
  }

  private fun ip(city: String, country: String = "ID") =
    """{"success":true,"country_code":"$country","city":"$city","latitude":-6.21,"longitude":106.84}"""

  private fun resolver(store: CityStore, body: String?) =
    LocationResolver(store, { body ?: throw IOException("offline") }, { cities })

  @Test fun detectsJakartaAndSavesCoordinates() {
    val store = MemoryStore()
    assertEquals("1301", resolver(store, ip("Jakarta")).detect()?.id)
    assertEquals("1301", store.cityId)
    assertEquals(-6.21, store.lat!!, 0.0)
    assertFalse(store.manual)
  }

  @Test fun manualChoiceIsNeverOverwritten() { // AE2
    val store = MemoryStore().apply { cityId = "1222"; cityName = "KOTA BOGOR"; manual = true }
    assertNull(resolver(store, ip("Bekasi")).detect())
    assertEquals("1222", store.cityId)
    assertTrue(store.manual)
  }

  @Test fun forcedRedetectReplacesManualOnUniqueMatch() {
    val store = MemoryStore().apply { cityId = "1222"; manual = true }
    assertEquals("1301", resolver(store, ip("Jakarta")).detect(force = true)?.id)
    assertFalse(store.manual)
  }

  @Test fun failedForcedRedetectKeepsManualChoice() {
    val store = MemoryStore().apply { cityId = "1222"; manual = true }
    assertNull(resolver(store, ip("Atlantis")).detect(force = true))
    assertEquals("1222", store.cityId)
    assertTrue(store.manual)
  }

  @Test fun foreignIpDoesNotMatch() {
    val store = MemoryStore()
    assertNull(resolver(store, ip("Bogor", country = "US")).detect())
    assertNull(store.cityId)
  }

  @Test fun ipServiceFailureLeavesCityUntouched() {
    val store = MemoryStore().apply { cityId = "1301" }
    assertNull(resolver(store, null).detect())
    assertEquals("1301", store.cityId)
  }
}
