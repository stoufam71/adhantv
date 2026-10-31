package id.adzantv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CityMatcherTest {
  private val cities = CityMatcher.parse(javaClass.classLoader!!.getResource("kota-semua.json")!!.readText())

  private fun id(ipCity: String) = CityMatcher.match(ipCity, cities)?.id

  @Test fun parsesAllCities() = assertEquals(518, cities.size)

  @Test fun jakarta() = assertEquals("1301", id("Jakarta"))

  @Test fun prefersKotaOverKab() = assertEquals("1222", id("Bogor"))

  @Test fun kabupatenInputMatchesKab() = assertEquals("KAB. BANDUNG", CityMatcher.match("Kabupaten Bandung", cities)?.name)

  @Test fun caseAndSpacesIgnored() = assertEquals("1222", id("  kota bogor "))

  @Test fun unknownCityNeedsPicker() = assertNull(id("Singapore"))
}
