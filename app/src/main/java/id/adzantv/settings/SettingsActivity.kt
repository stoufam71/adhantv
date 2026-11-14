package id.adzantv.settings

import android.content.ComponentName
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import id.adzantv.Adzan
import id.adzantv.adhan.AdhanScheduler
import id.adzantv.adhan.ReminderOverlay
import id.adzantv.data.City
import id.adzantv.data.LocationResolver
import id.adzantv.data.MyQuranClient
import id.adzantv.data.Prefs
import id.adzantv.data.loadCities
import id.adzantv.dream.AdzanDreamService
import id.adzantv.time.Prayer
import id.adzantv.time.PrayerClock
import id.adzantv.time.PrayerTimes
import id.adzantv.time.ReminderClock
import id.adzantv.ui.Palette
import id.adzantv.ui.Palettes
import id.adzantv.ui.SettingsScheme
import id.adzantv.ui.Stage
import id.adzantv.ui.paletteOf
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.concurrent.thread

private const val SCREENSAVER = "screensaver_components"

/**
 * Settings: city, adhan, reminder, screensaver and theme. First launch also detects the city and arms the
 * first alarms. This class holds the state and the actions; the screens are in the files beside it.
 */
class SettingsActivity : ComponentActivity() {
  private val prefs by lazy { Prefs(this) }
  private val cities = mutableStateOf<List<City>?>(null) // null = loading
  private val city = mutableStateOf<String?>(null)
  private val today = mutableStateOf<PrayerTimes?>(null)
  private val message = mutableStateOf<Message?>(null)
  private val busy = mutableStateOf(false)
  private val focusSearch = mutableStateOf(false)
  private val palette = mutableStateOf(Palettes[0])
  private val dreamOn = mutableStateOf(false)
  private val adhanOn = mutableStateOf(true)
  private val reminderOn = mutableStateOf(true)
  private val reminderMinutes = mutableStateOf(5)
  private val reminderPrayers = mutableStateOf(ReminderClock.ALL)
  private val dream by lazy { ComponentName(this, AdzanDreamService::class.java).flattenToShortString() }

  // worker-thread copy: cities.value is only updated later on the UI thread
  @Volatile private var loaded: List<City> = emptyList()
  private val resolver by lazy { LocationResolver(prefs, cities = { loaded }) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    city.value = prefs.cityName
    palette.value = paletteOf(prefs.palette)
    adhanOn.value = prefs.adhanOn
    reminderOn.value = prefs.reminderOn
    reminderMinutes.value = prefs.reminderMinutes
    reminderPrayers.value = prefs.reminderPrayers
    val actions = SettingsActions(
      retry = ::loadCityList,
      redetect = { detect(force = true) },
      pickCity = ::pick,
      testAdhan = ::testAdhan,
      pickPalette = ::choosePalette,
      toggleDream = ::toggleDream,
      toggleAdhan = ::toggleAdhan,
      toggleReminder = ::toggleReminder,
      setReminderMinutes = ::setReminderMinutes,
      toggleReminderPrayer = ::toggleReminderPrayer,
      previewReminder = ::previewReminder,
    )
    setContent {
      // the fixed settings colours; only the theme previews and the reminder sample provide a palette of their own
      MaterialTheme(colorScheme = SettingsScheme) {
        Stage(ornament = false) {
          SettingsScreens(
            SettingsState(
              cities = cities.value,
              city = city.value,
              today = today.value,
              message = message.value,
              busy = busy.value,
              focusSearch = focusSearch.value,
              palette = palette.value,
              dreamOn = dreamOn.value,
              adhanOn = adhanOn.value,
              reminderOn = reminderOn.value,
              reminderMinutes = reminderMinutes.value,
              reminderPrayers = reminderPrayers.value,
            ),
            actions,
          )
        }
      }
    }
    loadCityList()
  }

  override fun onResume() {
    super.onResume()
    // the screensaver can also be changed in the TV's own settings
    dreamOn.value = isOurDream()
  }

  /** [ok] false: a notice or a problem, shown without the success mark. */
  private fun say(text: String?, ok: Boolean = true) {
    message.value = text?.let { Message(it, ok) }
  }

  /** Off cancels the pending alarm; on arms the next prayer again. Test adzan works either way. */
  private fun toggleAdhan() {
    prefs.adhanOn = !prefs.adhanOn
    adhanOn.value = prefs.adhanOn
    say(if (adhanOn.value) "Adzan aktif kembali" else "Adzan dimatikan. Jadwal tetap tampil")
    work { Adzan.reschedule(this) }
  }

  private fun toggleReminder() {
    prefs.reminderOn = !prefs.reminderOn
    reminderOn.value = prefs.reminderOn
    say(if (reminderOn.value) "Pengingat ${prefs.reminderMinutes} menit sebelum adzan" else "Pengingat dimatikan")
    work { Adzan.reschedule(this) }
  }

  private fun setReminderMinutes(minutes: Int) {
    if (minutes == prefs.reminderMinutes) return
    prefs.reminderMinutes = minutes
    reminderMinutes.value = prefs.reminderMinutes
    say("Pengingat ${prefs.reminderMinutes} menit sebelum adzan")
    work { Adzan.reschedule(this) }
  }

  /** [prayer] null is "Semua shalat". The last selected prayer stays: an active reminder always has one. */
  private fun toggleReminderPrayer(prayer: Prayer?) {
    val before = prefs.reminderPrayers
    val after = ReminderClock.toggle(before, prayer)
    if (after == before) {
      if (prayer != null) say("Pilih minimal satu shalat", ok = false)
      return
    }
    prefs.reminderPrayers = after
    reminderPrayers.value = after
    say("Pengingat untuk ${prayersLabel(after).lowercase()}")
    work { Adzan.reschedule(this) }
  }

  /** Shows the real card now, for the next prayer, so it can be judged over whatever was on before. */
  private fun previewReminder() {
    work {
      val now = ZonedDateTime.now()
      val next = PrayerClock.next(Adzan.day(this, now.toLocalDate()), Adzan.day(this, now.toLocalDate().plusDays(1)), now)
      runOnUiThread {
        val shown = ReminderOverlay.show(this, next.prayer, next.at.toInstant().toEpochMilli(), prefs.reminderMinutes)
        if (shown) say("Contoh pengingat tampil 10 detik") else say("Izin belum diberikan: jalankan tools/install.sh sekali lagi", ok = false)
      }
    }
  }

  private fun choosePalette(p: Palette) {
    prefs.palette = p.id
    palette.value = p
    say("Tema ${p.name} dipakai")
  }

  private fun isOurDream() = Settings.Secure.getString(contentResolver, SCREENSAVER)?.contains(dream) == true

  /** On: our screensaver. Off: the TV's own default (Google's Ambient on this TV), never none. */
  private fun toggleDream() {
    val target = if (dreamOn.value) Settings.Secure.getString(contentResolver, "screensaver_default_component") else dream
    try {
      if (target.isNullOrEmpty()) {
        say("Screensaver bawaan TV tidak ditemukan", ok = false)
        return
      }
      Settings.Secure.putString(contentResolver, SCREENSAVER, target)
      dreamOn.value = isOurDream()
      say(if (dreamOn.value) "Screensaver Adzan TV aktif" else "Screensaver kembali ke bawaan TV. Adzan tetap berbunyi")
    } catch (e: SecurityException) {
      say("Izin belum diberikan: jalankan tools/install.sh sekali lagi", ok = false)
    }
  }

  private fun work(block: () -> Unit) {
    busy.value = true
    thread {
      try {
        block()
      } finally {
        runOnUiThread { busy.value = false }
      }
    }
  }

  private fun loadCityList() {
    cities.value = null
    work {
      val list = loadCities(filesDir, MyQuranClient())
      loaded = list
      runOnUiThread { cities.value = list }
      if (prefs.cityId == null) detect(force = false) else refresh()
    }
  }

  private fun detect(force: Boolean) {
    say("Mendeteksi lokasi…", ok = false)
    focusSearch.value = false // so a failure below changes it again and reopens the city list every time
    work {
      val found = resolver.detect(force)
      runOnUiThread {
        when {
          found != null -> say("Kota diubah ke ${found.name}")
          force || prefs.cityId == null -> {
            say("Kota tidak terdeteksi, pilih manual", ok = false)
            focusSearch.value = true
          }
          else -> say(null)
        }
      }
      refresh()
    }
  }

  private fun pick(c: City) {
    resolver.pick(c)
    say("Kota diubah ke ${c.name}")
    work { refresh() }
  }

  /** Off the main thread: reload today's times for the current city and re-arm the alarms. */
  private fun refresh() {
    val day = Adzan.day(this, LocalDate.now())
    Adzan.reschedule(this)
    runOnUiThread {
      city.value = prefs.cityName
      today.value = day
    }
  }

  private fun testAdhan() {
    work {
      val now = ZonedDateTime.now()
      val next = PrayerClock.next(Adzan.day(this, now.toLocalDate()), Adzan.day(this, now.toLocalDate().plusDays(1)), now)
      AdhanScheduler.set(this, System.currentTimeMillis() + 5_000, next.prayer, AdhanScheduler.TEST_SLOT)
      runOnUiThread { say("Adzan tes akan berbunyi dalam 5 detik") }
    }
  }
}
