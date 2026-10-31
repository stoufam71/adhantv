package id.adzantv.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
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
import id.adzantv.ui.IslamicPattern
import id.adzantv.ui.Ivory
import id.adzantv.ui.Lapis
import id.adzantv.ui.LapisDeep
import id.adzantv.ui.LineSoft
import id.adzantv.ui.Mist
import id.adzantv.ui.TvColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread

private val TIME = DateTimeFormatter.ofPattern("HH:mm")

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
      MaterialTheme(colorScheme = TvColors) {
        // Surface, not a bare background: it supplies the content color the focus state layer needs
        Surface(color = LapisDeep, modifier = Modifier.fillMaxSize()) {
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
      }
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
      AdhanScheduler.set(this, System.currentTimeMillis() + 5_000, next.prayer, slot = 1)
      runOnUiThread { message.value = "Adzan tes akan berbunyi dalam 5 detik" }
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

  // Afnon's settings look: whisper pattern over the lapis gradient, content in rounded cards
  Box(Modifier.fillMaxSize()) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Lapis, LapisDeep))))
    IslamicPattern(Modifier.fillMaxSize(), color = Gold.copy(alpha = 0.07f))
    Column(Modifier.fillMaxSize().padding(horizontal = 96.dp, vertical = 28.dp)) {
      Text("Adzan TV", color = Gold, fontSize = 20.sp, letterSpacing = 0.3.em)
      Text("Setelan", color = Mist, fontSize = 14.sp, letterSpacing = 0.2.em)
      Spacer(Modifier.height(12.dp))

      TvCard {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
          Text("SHALAT HARI INI", color = Mist, fontSize = 13.sp, letterSpacing = 0.25.em)
          Spacer(Modifier.height(4.dp))
          if (today == null) {
            Text("Memuat jadwal…", color = Mist, fontSize = 18.sp)
          } else {
            val next = Prayer.entries.firstOrNull { p -> p.obligatory && today.times[p]?.isAfter(LocalTime.now()) == true }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Prayer.entries.forEach { p ->
                val isNext = p == next
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  // a bar, not only gold text: marks the next prayer without relying on colour alone
                  Box(
                    Modifier
                      .padding(bottom = 4.dp)
                      .width(28.dp)
                      .height(2.dp)
                      .background(if (isNext) Gold else Color.Transparent, RoundedCornerShape(1.dp)),
                  )
                  Text(p.label, color = if (isNext) Gold else Mist, fontSize = 15.sp)
                  Text(today.times[p]?.format(TIME) ?: "--:--", color = if (isNext) Gold else Ivory, fontSize = 22.sp)
                }
              }
            }
            if (today.estimated) {
              Spacer(Modifier.height(8.dp))
              Text("perkiraan — jadwal Kemenag belum tersedia", color = Mist, fontSize = 14.sp)
            }
          }
        }
      }
      Spacer(Modifier.height(12.dp))

      TvCard(Modifier.weight(1f)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
          // city and actions share one row so the city list keeps room on a 540dp-tall TV
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
              Text("KOTA", color = Mist, fontSize = 13.sp, letterSpacing = 0.25.em)
              Text(city ?: "belum diatur", color = Ivory, fontSize = 22.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
              TvButton(if (busy) "Memproses…" else "Deteksi ulang", onRedetect, Modifier.focusRequester(buttonFocus))
              TvButton("Tes adzan (5 detik)", onTest)
            }
          }
          if (message != null) {
            Spacer(Modifier.height(4.dp))
            Text(message, color = Gold, fontSize = 16.sp)
          }
          Spacer(Modifier.height(8.dp))

          when {
            cities == null -> Text("Memuat daftar kota…", color = Ivory)
            cities.isEmpty() -> {
              Text("Tidak ada koneksi, coba lagi", color = Ivory)
              Spacer(Modifier.height(8.dp))
              TvButton("Coba lagi", onRetry)
            }
            else -> {
              OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Cari kabupaten/kota", fontSize = 16.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 20.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth().focusRequester(searchFocus),
              )
              val shown = cities.filter { it.name.contains(query.trim(), ignoreCase = true) }
              if (shown.isEmpty()) Text("Kota tidak ditemukan", color = Mist, modifier = Modifier.padding(top = 8.dp))
              LazyColumn(Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp)) {
                items(shown, key = { it.id }) { c -> CityRow(c.name, selected = c.name == city) { onPick(c) } }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TvCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  Column(
    modifier
      .background(Lapis, RoundedCornerShape(16.dp))
      .border(1.dp, LineSoft, RoundedCornerShape(16.dp)),
  ) {
    content()
  }
}

@Composable
private fun TvButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
  var focused by remember { mutableStateOf(false) }
  // colour change only, no bounds-scale: a shifting layout makes the D-pad feel unstable
  Text(
    label,
    color = if (focused) LapisDeep else Ivory,
    fontSize = 20.sp,
    modifier = modifier
      .onFocusChanged { focused = it.isFocused }
      .background(if (focused) Gold else Lapis, RoundedCornerShape(8.dp))
      .border(1.dp, if (focused) Color.Transparent else LineSoft, RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 28.dp, vertical = 13.dp),
  )
}

@Composable
private fun CityRow(name: String, selected: Boolean, onClick: () -> Unit) {
  var focused by remember { mutableStateOf(false) }
  Row(
    Modifier
      .fillMaxWidth()
      .onFocusChanged { focused = it.isFocused }
      .background(if (focused) Gold else Color.Transparent, RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // a bar, not a tick glyph: marks the chosen city without relying on a font
    Box(
      Modifier
        .width(3.dp)
        .height(16.dp)
        .background(if (selected) Gold else Color.Transparent, RoundedCornerShape(2.dp)),
    )
    Spacer(Modifier.width(9.dp))
    Text(name, color = if (focused) LapisDeep else if (selected) Gold else Ivory, fontSize = 20.sp)
  }
}
