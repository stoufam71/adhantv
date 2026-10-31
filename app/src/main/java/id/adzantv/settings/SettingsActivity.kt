package id.adzantv.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.adzantv.Adzan
import id.adzantv.adhan.AdhanScheduler
import id.adzantv.data.City
import id.adzantv.data.LocationResolver
import id.adzantv.data.MyQuranClient
import id.adzantv.data.Prefs
import id.adzantv.data.loadCities
import id.adzantv.time.Prayer
import id.adzantv.time.PrayerClock
import id.adzantv.time.PrayerTimes
import id.adzantv.ui.Gold
import id.adzantv.ui.Ivory
import id.adzantv.ui.Lapis
import id.adzantv.ui.LapisDeep
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread

/** City choice (search or re-detect) and a test adhan. First launch also detects the city and arms the first alarm. */
class SettingsActivity : ComponentActivity() {
  private val prefs by lazy { Prefs(this) }
  private val cities = mutableStateOf<List<City>?>(null) // null = loading
  private val city = mutableStateOf<String?>(null)
  private val today = mutableStateOf<PrayerTimes?>(null)
  private val message = mutableStateOf<String?>(null)
  private val busy = mutableStateOf(false)
  private val focusSearch = mutableStateOf(false)

  // worker-thread copy: cities.value is only updated later on the UI thread
  @Volatile private var loaded: List<City> = emptyList()
  private val resolver by lazy { LocationResolver(prefs, cities = { loaded }) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    city.value = prefs.cityName
    setContent {
      Screen(
        cities = cities.value,
        city = city.value,
        today = today.value,
        message = message.value,
        busy = busy.value,
        focusSearch = focusSearch.value,
        onRetry = ::loadCityList,
        onRedetect = { detect(force = true) },
        onPick = ::pick,
        onTest = ::testAdhan,
      )
    }
    loadCityList()
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
    message.value = "Mendeteksi lokasi…"
    work {
      val found = resolver.detect(force)
      runOnUiThread {
        when {
          found != null -> message.value = "Kota diubah ke ${found.name}"
          force || prefs.cityId == null -> {
            message.value = "Kota tidak terdeteksi, pilih manual"
            focusSearch.value = true
          }
          else -> message.value = null
        }
      }
      refresh()
    }
  }

  private fun pick(c: City) {
    resolver.pick(c)
    message.value = "Kota diubah ke ${c.name}"
    work { refresh() }
  }

  /** Off the main thread: reload today's times for the current city and re-arm the alarm. */
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
      AdhanScheduler.set(this, System.currentTimeMillis() + 60_000, next.prayer, slot = 1)
      runOnUiThread { message.value = "Adzan tes akan berbunyi dalam 1 menit" }
    }
  }
}

@Composable
private fun Screen(
  cities: List<City>?,
  city: String?,
  today: PrayerTimes?,
  message: String?,
  busy: Boolean,
  focusSearch: Boolean,
  onRetry: () -> Unit,
  onRedetect: () -> Unit,
  onPick: (City) -> Unit,
  onTest: () -> Unit,
) {
  var query by remember { mutableStateOf("") }
  val searchFocus = remember { FocusRequester() }
  // start on the buttons; landing on the search field would pop the keyboard every launch
  val buttonFocus = remember { FocusRequester() }
  LaunchedEffect(Unit) { buttonFocus.requestFocus() }
  LaunchedEffect(focusSearch, cities) { if (focusSearch && !cities.isNullOrEmpty()) searchFocus.requestFocus() }

  Column(Modifier.fillMaxSize().background(LapisDeep).padding(horizontal = 48.dp, vertical = 27.dp)) {
    Text("Adzan TV", color = Gold, fontSize = 32.sp)
    Text("Kota: ${city ?: "belum diatur"}", color = Ivory, fontSize = 22.sp)
    if (today != null && city != null) {
      Text(
        Prayer.entries.joinToString("   ") { "${it.label} ${today.times[it]?.format(DateTimeFormatter.ofPattern("HH:mm"))}" } +
          if (today.estimated) "   (perkiraan)" else "",
        color = Ivory.copy(alpha = 0.7f),
        fontSize = 18.sp,
      )
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      TvButton(if (busy) "Memproses…" else "Deteksi ulang", onRedetect, Modifier.focusRequester(buttonFocus))
      TvButton("Tes adzan (1 menit)", onTest)
    }
    if (message != null) Text(message, color = Gold, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
    Spacer(Modifier.height(16.dp))

    when {
      cities == null -> Text("Memuat daftar kota…", color = Ivory)
      cities.isEmpty() -> {
        Text("Tidak ada koneksi, coba lagi", color = Ivory)
        TvButton("Coba lagi", onRetry)
      }
      else -> {
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          label = { Text("Cari kabupaten/kota") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
          modifier = Modifier.fillMaxWidth().focusRequester(searchFocus),
        )
        val shown = cities.filter { it.name.contains(query.trim(), ignoreCase = true) }
        if (shown.isEmpty()) Text("Kota tidak ditemukan", color = Ivory, modifier = Modifier.padding(top = 8.dp))
        LazyColumn(Modifier.fillMaxWidth().padding(top = 8.dp)) {
          items(shown, key = { it.id }) { c -> CityRow(c.name, selected = c.name == city) { onPick(c) } }
        }
      }
    }
  }
}

@Composable
private fun TvButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
  var focused by remember { mutableStateOf(false) }
  Text(
    label,
    color = if (focused) LapisDeep else Ivory,
    fontSize = 20.sp,
    modifier = modifier
      .onFocusChanged { focused = it.isFocused }
      .background(if (focused) Gold else Lapis, RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 20.dp, vertical = 10.dp),
  )
}

@Composable
private fun CityRow(name: String, selected: Boolean, onClick: () -> Unit) {
  var focused by remember { mutableStateOf(false) }
  Text(
    (if (selected) "✓ " else "   ") + name,
    color = if (focused) LapisDeep else Ivory,
    fontSize = 20.sp,
    modifier = Modifier
      .fillMaxWidth()
      .onFocusChanged { focused = it.isFocused }
      .background(if (focused) Gold else Color.Transparent, RoundedCornerShape(6.dp))
      .border(if (selected) 1.dp else 0.dp, Gold.copy(alpha = if (selected) 0.6f else 0f), RoundedCornerShape(6.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 8.dp),
  )
}
