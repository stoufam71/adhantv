package id.adzantv.settings

import android.content.ComponentName
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
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
import id.adzantv.dream.AdzanDreamService
import id.adzantv.dream.Screensaver
import id.adzantv.dream.ScreensaverData
import id.adzantv.time.Prayer
import id.adzantv.time.PrayerClock
import id.adzantv.time.PrayerTimes
import id.adzantv.ui.Display
import id.adzantv.ui.Gold
import id.adzantv.ui.Ground
import id.adzantv.ui.Ivory
import id.adzantv.ui.LineSoft
import id.adzantv.ui.LocalPalette
import id.adzantv.ui.Mihrab
import id.adzantv.ui.Mist
import id.adzantv.ui.Palette
import id.adzantv.ui.Palettes
import id.adzantv.ui.Stage
import id.adzantv.ui.Surface
import id.adzantv.ui.TvColors
import id.adzantv.ui.paletteOf
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread

private val TIME = DateTimeFormatter.ofPattern("HH:mm")
private const val SCREENSAVER = "screensaver_components"

/** City choice (search or re-detect) and a test adhan. First launch also detects the city and arms the first alarm. */
class SettingsActivity : ComponentActivity() {
  private val prefs by lazy { Prefs(this) }
  private val cities = mutableStateOf<List<City>?>(null) // null = loading
  private val city = mutableStateOf<String?>(null)
  private val today = mutableStateOf<PrayerTimes?>(null)
  private val message = mutableStateOf<String?>(null)
  private val busy = mutableStateOf(false)
  private val focusSearch = mutableStateOf(false)
  private val palette = mutableStateOf(Palettes[0])
  private val dreamOn = mutableStateOf(false)
  private val dream by lazy { ComponentName(this, AdzanDreamService::class.java).flattenToShortString() }

  // worker-thread copy: cities.value is only updated later on the UI thread
  @Volatile private var loaded: List<City> = emptyList()
  private val resolver by lazy { LocationResolver(prefs, cities = { loaded }) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    city.value = prefs.cityName
    palette.value = paletteOf(prefs.palette)
    setContent {
      CompositionLocalProvider(LocalPalette provides palette.value) {
        MaterialTheme(colorScheme = TvColors) {
          Box(Modifier.fillMaxSize().background(Ground)) {
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
              palette = palette.value,
              onPalette = ::choosePalette,
              dreamOn = dreamOn.value,
              onDream = ::toggleDream,
            )
          }
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

  private fun choosePalette(p: Palette) {
    prefs.palette = p.id
    palette.value = p
    message.value = "Tema ${p.name} dipakai"
  }

  private fun isOurDream() = Settings.Secure.getString(contentResolver, SCREENSAVER)?.contains(dream) == true

  /** On: our screensaver. Off: the TV's own default (Google's Ambient on this TV), never none. */
  private fun toggleDream() {
    val target = if (dreamOn.value) Settings.Secure.getString(contentResolver, "screensaver_default_component") else dream
    try {
      if (target.isNullOrEmpty()) {
        message.value = "Screensaver bawaan TV tidak ditemukan"
        return
      }
      Settings.Secure.putString(contentResolver, SCREENSAVER, target)
      dreamOn.value = isOurDream()
      message.value = if (dreamOn.value) "Screensaver Adzan TV aktif" else "Screensaver kembali ke bawaan TV; adzan tetap berbunyi"
    } catch (e: SecurityException) {
      message.value = "Izin belum diberikan: jalankan tools/install.sh sekali lagi"
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


/** Afnon's focus: gold fill, an ivory ring 6dp outside, a small lift. Fast (120ms) so the D-pad feels direct. */
private val FOCUS_MS = 120

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
  palette: Palette,
  onPalette: (Palette) -> Unit,
  dreamOn: Boolean,
  onDream: () -> Unit,
) {
  var choosing by remember { mutableStateOf(false) }
  var theming by remember { mutableStateOf(false) }
  val first = remember { FocusRequester() }
  val themeRow = remember { FocusRequester() }
  // start on the first row; landing on a text field would pop the keyboard every launch.
  // Closing a chooser returns focus to the row that opened it, so Back never strands the D-pad.
  LaunchedEffect(choosing) { if (!choosing) first.requestFocus() }
  var themed by remember { mutableStateOf(false) }
  LaunchedEffect(theming) {
    if (theming) themed = true else if (themed) themeRow.requestFocus()
  }
  // detection failed and no city yet: go straight to the chooser
  LaunchedEffect(focusSearch, cities) { if (focusSearch && !cities.isNullOrEmpty()) choosing = true }

  Stage {
    // an open chooser replaces the rows rather than covering them: hidden rows would still take D-pad focus
    val overlay = theming || (choosing && !cities.isNullOrEmpty())
    if (!overlay) {
      Sidebar(today)
      Text("ADZAN TV", color = Mist, fontSize = 22.sp, letterSpacing = 0.12.em, modifier = Modifier.offset(640.dp, 56.dp))
      Column(Modifier.offset(640.dp, 112.dp).width(1184.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingRow(
          "Lokasi",
          "Jadwal shalat Kemenag untuk kota ini",
          value = city ?: "Belum diatur",
          chevron = true,
          modifier = Modifier.focusRequester(first),
        ) { if (!cities.isNullOrEmpty()) choosing = true }
        SettingRow("Deteksi ulang lokasi", "Mencari kota dari alamat internet", value = if (busy) "Memproses…" else null) { onRedetect() }
        SettingRow(
          "Screensaver Adzan TV",
          if (dreamOn) "Tampil saat TV diam; OK untuk memakai screensaver bawaan" else "Memakai screensaver bawaan TV; adzan tetap berbunyi",
          value = if (dreamOn) "Aktif" else "Mati",
        ) { onDream() }
        SettingRow(
          "Tema warna",
          "Warna layar untuk ruangan gelap",
          value = palette.name,
          chevron = true,
          modifier = Modifier.focusRequester(themeRow),
        ) { theming = true }
        SettingRow("Tes adzan", "Adzan berbunyi 5 detik setelah ditekan", value = null) { onTest() }
        when {
          cities == null -> Status("Memuat daftar kota…")
          cities.isEmpty() -> {
            Status("Tidak ada koneksi internet")
            SettingRow("Coba lagi", "Memuat ulang daftar kota", value = null) { onRetry() }
          }
        }
        if (message != null) Status(message)
      }
      Text(
        "▲▼  Pindah        OK  Pilih        Kembali  Keluar",
        color = Mist,
        fontSize = 22.sp,
        modifier = Modifier.offset(640.dp, 976.dp),
      )
    }
    if (theming) {
      // the chooser previews the real screensaver, with today's times when they are loaded
      val preview = today?.let { ScreensaverData(city, it, it.copy(date = it.date.plusDays(1))) }
      ThemeChooser(palette, preview, onClose = { theming = false }) {
        onPalette(it)
        theming = false
      }
    }
    if (choosing && !cities.isNullOrEmpty()) {
      Chooser(cities, city, onClose = { choosing = false }) {
        onPick(it)
        choosing = false
      }
    }
  }
}

/** Afnon's settings sidebar: the title under a small mihrab, then today's times so a change shows at once. */
@Composable
private fun Sidebar(today: PrayerTimes?) {
  Box(Modifier.width(560.dp).fillMaxHeight().background(Ground)) {
    Row(Modifier.offset(96.dp, 56.dp), verticalAlignment = Alignment.CenterVertically) {
      Mihrab(Modifier.size(40.dp, 60.dp), stroke = 2.dp)
      Spacer(Modifier.width(20.dp))
      Text("Setelan", color = Ivory, fontSize = 52.sp, fontFamily = Display, fontWeight = FontWeight.Bold)
    }
    Column(Modifier.offset(96.dp, 180.dp).width(424.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text("SHALAT HARI INI", color = Mist, fontSize = 20.sp, letterSpacing = 0.12.em, modifier = Modifier.padding(bottom = 6.dp))
      if (today == null) {
        Text("Memuat jadwal…", color = Mist, fontSize = 26.sp)
      } else {
        val next = Prayer.entries.firstOrNull { p -> p.obligatory && today.times[p]?.isAfter(LocalTime.now()) == true }
        Prayer.entries.forEach { p ->
          val isNext = p == next
          val shape = RoundedCornerShape(14.dp)
          Row(
            Modifier
              .fillMaxWidth()
              .height(76.dp)
              .then(if (isNext) Modifier.border(3.dp, Gold, shape) else Modifier.background(Surface, shape))
              .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(p.label, color = if (isNext) Gold else Ivory, fontSize = 26.sp, modifier = Modifier.weight(1f))
            // a word, not only gold: marks the next prayer without relying on colour alone
            if (isNext) Text("BERIKUT  ", color = Gold, fontSize = 18.sp, letterSpacing = 0.12.em)
            Text(
              today.times[p]?.format(TIME) ?: "--:--",
              color = if (isNext) Gold else Ivory,
              fontSize = 30.sp,
              fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Normal,
            )
          }
        }
        if (today.estimated) Text("Perkiraan: jadwal Kemenag belum tersedia", color = Mist, fontSize = 20.sp)
      }
    }
  }
}

/** One D-pad row: label and hint on the left, the current value on the right. */
@Composable
private fun SettingRow(
  label: String,
  hint: String,
  value: String?,
  modifier: Modifier = Modifier,
  chevron: Boolean = false,
  onClick: () -> Unit,
) {
  Focusable(modifier.fillMaxWidth().height(100.dp), onClick = onClick) { focused ->
    val fg = if (focused) Ground else Ivory
    Row(Modifier.fillMaxSize().padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text(label, color = fg, fontSize = 28.sp, fontWeight = FontWeight.Medium)
        Text(hint, color = if (focused) Ground.copy(alpha = 0.75f) else Mist, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      if (value != null) Text(value, color = if (focused) Ground else Gold, fontSize = 26.sp, maxLines = 1)
      if (chevron) {
        Spacer(Modifier.width(12.dp))
        Chevron(fg)
      }
    }
  }
}

/** Afnon's focus treatment for any D-pad target; colour and scale only, so neighbours never move. */
@Composable
private fun Focusable(
  modifier: Modifier,
  onClick: () -> Unit,
  content: @Composable (focused: Boolean) -> Unit,
) {
  var focused by remember { mutableStateOf(false) }
  val scale by animateFloatAsState(if (focused) 1.02f else 1f, tween(FOCUS_MS), label = "focus")
  Box(
    modifier
      .graphicsLayer { scaleX = scale; scaleY = scale }
      .onFocusChanged { focused = it.isFocused }
      .border(4.dp, if (focused) Ivory else Color.Transparent, RoundedCornerShape(22.dp)) // the fill radius plus the 6dp gap
      .padding(6.dp)
      .background(if (focused) Gold else Surface, RoundedCornerShape(16.dp))
      .clickable(onClick = onClick),
  ) {
    content(focused)
  }
}

@Composable
private fun Status(text: String) {
  Text(text, color = Gold, fontSize = 24.sp, modifier = Modifier.padding(start = 34.dp, top = 6.dp))
}

/** A drawn chevron, not a font glyph: same weight on every TV. */
@Composable
private fun Chevron(color: Color) {
  Canvas(Modifier.size(14.dp, 24.dp)) {
    val w = 3.dp.toPx()
    drawLine(color, Offset(0f, 0f), Offset(size.width, size.height / 2), w, StrokeCap.Round)
    drawLine(color, Offset(size.width, size.height / 2), Offset(0f, size.height), w, StrokeCap.Round)
  }
}

/**
 * Full-screen city chooser on a solid ground, so nothing behind competes with the list. Opens on the current city so OK-OK keeps it; Up reaches the
 * search field for voice or keyboard; Back closes without changing anything.
 */
@Composable
private fun Chooser(cities: List<City>, current: String?, onClose: () -> Unit, onPick: (City) -> Unit) {
  BackHandler(onBack = onClose)
  var query by remember { mutableStateOf("") }
  val shown = cities.filter { it.name.contains(query.trim(), ignoreCase = true) }
  val list = rememberLazyListState()
  val start = remember { FocusRequester() }
  val search = remember { FocusRequester() }
  val at = cities.indexOfFirst { it.name == current }
  LaunchedEffect(Unit) {
    if (at < 0) {
      search.requestFocus()
    } else {
      list.scrollToItem(maxOf(at - 3, 0))
      start.requestFocus()
    }
  }
  Box(Modifier.fillMaxSize().background(Ground)) {
    Column(Modifier.offset(460.dp, 72.dp).width(1000.dp).fillMaxHeight().padding(bottom = 72.dp)) {
      Text("Pilih kota", color = Ivory, fontSize = 52.sp, fontFamily = Display, fontWeight = FontWeight.Bold)
      Spacer(Modifier.height(24.dp))
      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("Cari kabupaten/kota", fontSize = 22.sp) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        textStyle = LocalTextStyle.current.copy(fontSize = 28.sp, color = Ivory),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth().focusRequester(search),
      )
      Spacer(Modifier.height(16.dp))
      if (shown.isEmpty()) Text("Kota tidak ditemukan", color = Mist, fontSize = 26.sp, modifier = Modifier.padding(start = 34.dp))
      LazyColumn(state = list, modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        itemsIndexed(shown, key = { _, c -> c.id }) { _, c ->
          val selected = c.name == current
          Focusable(
            Modifier.fillMaxWidth().height(84.dp).then(if (selected && query.isBlank()) Modifier.focusRequester(start) else Modifier),
            onClick = { onPick(c) },
          ) { focused ->
            Row(Modifier.fillMaxSize().padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically) {
              Text(c.name, color = if (focused) Ground else Ivory, fontSize = 28.sp, modifier = Modifier.weight(1f))
              // a word, not only colour: marks the chosen city
              if (selected) Text("DIPILIH", color = if (focused) Ground else Gold, fontSize = 20.sp, letterSpacing = 0.12.em)
            }
          }
        }
      }
    }
  }
}

/** Each palette as a live, scaled-down screensaver, two by two; OK picks, Back keeps the current one. */
@Composable
private fun ThemeChooser(current: Palette, preview: ScreensaverData?, onClose: () -> Unit, onPick: (Palette) -> Unit) {
  BackHandler(onBack = onClose)
  val start = remember { FocusRequester() }
  LaunchedEffect(Unit) { start.requestFocus() }
  Box(Modifier.fillMaxSize().background(Ground)) {
    Column(Modifier.offset(340.dp, 56.dp).width(1240.dp)) {
      Text("Tema warna", color = Ivory, fontSize = 52.sp, fontFamily = Display, fontWeight = FontWeight.Bold)
      Text("Semua tema gelap dan redup untuk ruangan tanpa lampu", color = Mist, fontSize = 24.sp)
      Spacer(Modifier.height(32.dp))
      Palettes.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp), modifier = Modifier.padding(bottom = 28.dp)) {
          row.forEach { p ->
            val isCurrent = p.id == current.id
            Focusable(
              Modifier.width(600.dp).then(if (isCurrent) Modifier.focusRequester(start) else Modifier),
              onClick = { onPick(p) },
            ) { focused ->
              Column {
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))) {
                  CompositionLocalProvider(LocalPalette provides p) { Screensaver(preview) }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                  Text(p.name, color = if (focused) Ground else Ivory, fontSize = 26.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                  // a word, not only colour: marks the palette in use
                  if (isCurrent) Text("DIPAKAI", color = if (focused) Ground else Gold, fontSize = 20.sp, letterSpacing = 0.12.em)
                }
              }
            }
          }
        }
      }
    }
  }
}
