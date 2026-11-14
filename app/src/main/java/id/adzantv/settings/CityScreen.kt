package id.adzantv.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.adzantv.data.City
import id.adzantv.ui.SettingsColors as C

/**
 * The city list. Opens on the current city so OK-OK keeps it; Up reaches automatic detection and the
 * search field for voice or keyboard; Back closes without changing anything.
 */
@Composable
internal fun CityScreen(
  cities: List<City>,
  current: String?,
  busy: Boolean,
  onClose: () -> Unit,
  onRedetect: () -> Unit,
  onPick: (City) -> Unit,
) {
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
      list.scrollToItem(maxOf(at - 2, 0))
      start.requestFocus()
    }
  }
  Column(Modifier.offset(420.dp, 56.dp).width(1080.dp).fillMaxHeight().padding(bottom = 196.dp)) {
    Row(verticalAlignment = Alignment.Bottom) {
      Title("Pilih kota", Modifier.weight(1f))
      Text("${cities.size} kota", color = C.Dim, fontSize = 26.sp, modifier = Modifier.padding(bottom = 10.dp))
    }
    Spacer(Modifier.height(20.dp))
    OutlinedTextField(
      value = query,
      onValueChange = { query = it },
      placeholder = { Text("Cari nama kota", fontSize = 34.sp, color = C.Dim) },
      leadingIcon = { Icon(SEARCH, C.Dim, Modifier.padding(start = 16.dp).size(36.dp)) },
      singleLine = true,
      shape = RoundedCornerShape(18.dp),
      textStyle = LocalTextStyle.current.copy(fontSize = 34.sp, color = C.Text),
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
      modifier = Modifier.fillMaxWidth().focusRequester(search),
    )
    Spacer(Modifier.height(16.dp))
    SettingRow(
      TARGET,
      "Deteksi otomatis",
      hint = "Cari kota dari sambungan internet",
      value = if (busy) "Memproses…" else null,
    ) { onRedetect() }
    Spacer(Modifier.height(16.dp))
    if (shown.isEmpty()) Text("Kota tidak ditemukan", color = C.Dim, fontSize = 30.sp, modifier = Modifier.padding(start = 36.dp))
    LazyColumn(state = list, modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      itemsIndexed(shown, key = { _, c -> c.id }) { _, c ->
        val selected = c.name == current
        FocusBox(
          Modifier.fillMaxWidth().height(96.dp).then(if (selected && query.isBlank()) Modifier.focusRequester(start) else Modifier),
          onClick = { onPick(c) },
        ) { focused ->
          val fg = if (focused) C.Ground else C.Text
          Row(Modifier.fillMaxSize().padding(horizontal = 36.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(c.name, color = fg, fontSize = 34.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
            // a word and a mark, not only colour: the city in use
            if (selected) {
              Icon(CHECK, if (focused) C.Ground else C.On, stroke = 2.4f)
              Text("dipilih", color = if (focused) C.Ground else C.On, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 14.dp))
            }
          }
        }
      }
    }
  }
  Hints(Modifier.offset(420.dp, 968.dp), "Panah" to "pindah", "OK" to "pilih", "Kembali" to "batal")
}
