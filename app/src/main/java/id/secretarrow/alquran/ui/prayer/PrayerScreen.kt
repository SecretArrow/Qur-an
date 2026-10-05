package id.secretarrow.alquran.ui.prayer

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.secretarrow.alquran.core.HijriHelper
import id.secretarrow.alquran.di.AppContainer
import id.secretarrow.alquran.prayer.IndonesianCities
import id.secretarrow.alquran.prayer.PrayerCalculator
import id.secretarrow.alquran.ui.navigation.AppViewModelFactory
import id.secretarrow.alquran.ui.theme.GreenDark
import id.secretarrow.alquran.ui.theme.Teal
import java.time.ZoneId

/** Layar jadwal sholat hari ini + alarm adzan + pintasan kiblat. */
@Composable
fun PrayerScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenQibla: () -> Unit,
    onOpenCalendar: () -> Unit
) {
    val factory =
        remember(container) {
            AppViewModelFactory(container) {
                PrayerViewModelFactoryProvider(container)
            }
        }
    val viewModel: PrayerViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { grants ->
            if (grants.values.any { it }) {
                viewModel.refresh()
            } else {
                viewModel.markPermissionDenied()
            }
        }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )
    }

    var locationDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                title = { Text("Jadwal Sholat", color = Color.White, style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = onOpenCalendar) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "Kalender 30 hari", tint = Color.White)
                    }
                    IconButton(onClick = { locationDialog = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Pengaturan lokasi", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenDark)
            )
        }
    ) { padding ->
        if (state.loading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Teal) }
        } else {
            val zone = ZoneId.systemDefault()
            val times = state.times
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
            ) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(state.hijri, fontWeight = FontWeight.Medium)
                            Text(state.gregorian, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Menuju Waktu ${state.nextLabel.substringBefore(" ±")}",
                                    color = id.secretarrow.alquran.ui.theme.GoldText,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "± ${state.nextLabel.substringAfter("± ").substringBefore(" menit")} menit lagi",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = onOpenQibla,
                                shape = RoundedCornerShape(50),
                                colors =
                                    androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = Color.Black,
                                        contentColor = Color.White
                                    )
                            ) {
                                Text("Qiblat  ›")
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 10.dp)
                        ) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = Teal,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = "Wilayah: ${state.locationLabel.ifEmpty { "Jakarta, DKI Jakarta - Indonesia" }}",
                                fontSize = 13.sp
                            )
                        }
                        if (state.permissionDenied) {
                            Text(
                                text = "Izin lokasi ditolak — memakai kota bawaan. Ubah lewat ikon pengaturan.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }

                val times2 = times
                if (times2 != null) {
                    val entries =
                        listOf(
                            "Imsak" to times2.imsak,
                            "Subuh" to times2.subuh,
                            "Terbit" to times2.terbit,
                            "Dzuhur" to times2.dzuhur,
                            "Ashar" to times2.ashar,
                            "Maghrib" to times2.maghrib,
                            "Isya" to times2.isya
                        )
                    items(entries, key = { it.first }) { (name, millis) ->
                        val currentMillis = System.currentTimeMillis()
                        val isNow = currentMillis >= millis && currentMillis < millis + 30 * 60_000L
                        val toggleKey = name.uppercase().replace(" ", "")
                        val enabled =
                            when (toggleKey) {
                                "IMSAK" -> state.adzanToggles.imsak
                                "SUBUH" -> state.adzanToggles.subuh
                                "TERBIT" -> state.adzanToggles.terbit
                                "DZUHUR" -> state.adzanToggles.dzuhur
                                "ASHAR" -> state.adzanToggles.ashar
                                "MAGHRIB" -> state.adzanToggles.maghrib
                                "ISYA" -> state.adzanToggles.isya
                                else -> false
                            }
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 2.dp)
                                    .background(
                                        if (isNow) id.secretarrow.alquran.ui.theme.OrnamentBg else MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(8.dp)
                                    ).padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = name,
                                color = if (isNow) id.secretarrow.alquran.ui.theme.GoldText else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp
                            )
                            Box(Modifier.weight(1f))
                            Text(
                                text = PrayerCalculator.formatTime(millis, zone),
                                color = if (isNow) id.secretarrow.alquran.ui.theme.GoldText else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            IconButton(
                                onClick = { viewModel.toggleAdzan(toggleKey, !enabled) },
                                enabled = toggleKey != "IMSAK" && toggleKey != "TERBIT"
                            ) {
                                Icon(
                                    if (enabled) Icons.Filled.Alarm else Icons.Filled.AlarmOff,
                                    contentDescription = if (enabled) "Alarm aktif" else "Alarm mati",
                                    tint = if (enabled) Teal else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                item {
                    Text(
                        text = "Alarm memakai waktu sholat konvensi Kemenag (Subuh 20°, Isya 18°).\nImsak & Terbit tanpa alarm adzan.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    if (locationDialog) {
        LocationDialog(
            useGps = state.useGps,
            cityId = state.cityId,
            onUseGps = { viewModel.setUseGps(it) },
            onPickCity = {
                viewModel.setCity(it)
                locationDialog = false
            },
            onDismiss = { locationDialog = false }
        )
    }
}

/** Pembungkus factory khusus karena PrayerViewModel butuh Context. */
class PrayerViewModelFactoryProvider(
    private val container: AppContainer
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
        PrayerViewModel(container.appContext, container.settingsRepository) as T
}

@Composable
private fun LocationDialog(
    useGps: Boolean,
    cityId: String,
    onUseGps: (Boolean) -> Unit,
    onPickCity: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val selected = IndonesianCities.byId(cityId)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lokasi Jadwal Sholat") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Gunakan GPS otomatis", modifier = Modifier.weight(1f))
                    Switch(checked = useGps, onCheckedChange = onUseGps)
                }
                Box {
                    TextButton(onClick = { menuOpen = true }) {
                        Text("Pilih kota: ${selected.name}, ${selected.region}")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        IndonesianCities.cities.forEach { city ->
                            DropdownMenuItem(
                                text = { Text("${city.name}, ${city.region}") },
                                onClick = { onPickCity(city.id) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Selesai") }
        }
    )
}

/** Tabel jadwal 30 hari ke depan. */
@Composable
fun PrayerCalendarScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val factory =
        remember(container) {
            AppViewModelFactory(container) {
                PrayerViewModel(container.appContext, container.settingsRepository)
            }
        }
    val viewModel: PrayerViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()
    val zone = ZoneId.systemDefault()
    var showDetail by remember { mutableStateOf(true) }

    val rows =
        remember(state.lat, state.lng, state.date) {
            PrayerCalculator.calculateRange(state.lat, state.lng, state.date, 30, zone)
        }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                title = { Text("Jadwal 30 Hari Kedepan", color = Color.White, style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { showDetail = !showDetail }) {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = if (showDetail) "Mode tabel" else "Mode kartu",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenDark)
            )
        }
    ) { padding ->
        if (rows.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Teal) }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
            ) {
                item {
                    Row(
                        modifier =
                            Modifier
                                .background(id.secretarrow.alquran.ui.theme.GreenDarkDeep)
                                .padding(vertical = 8.dp)
                                .horizontalScroll(rememberScrollState())
                    ) {
                        CalendarCell("Tanggal", 90, header = true)
                        CalendarCell("Hijriah", 110, header = true)
                        CalendarCell("Imsak", 62, header = true)
                        CalendarCell("Subuh", 62, header = true)
                        CalendarCell("Terbit", 62, header = true)
                        CalendarCell("Dzuhur", 68, header = true)
                        CalendarCell("Ashar", 62, header = true)
                        CalendarCell("Maghrib", 70, header = true)
                        CalendarCell("Isya", 62, header = true)
                    }
                }
                items(rows, key = { it.first.toEpochDay() }) { (date, t) ->
                    Row(
                        modifier =
                            Modifier
                                .background(
                                    if (date.dayOfYear % 2 ==
                                        0
                                    ) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                ).horizontalScroll(rememberScrollState())
                    ) {
                        CalendarCell(HijriHelper.formatGregorianShort(date), 90)
                        CalendarCell(HijriHelper.format(date).removeSuffix(" H"), 110)
                        CalendarCell(PrayerCalculator.formatTime(t.imsak, zone), 62)
                        CalendarCell(PrayerCalculator.formatTime(t.subuh, zone), 62)
                        CalendarCell(PrayerCalculator.formatTime(t.terbit, zone), 62)
                        CalendarCell(PrayerCalculator.formatTime(t.dzuhur, zone), 68)
                        CalendarCell(PrayerCalculator.formatTime(t.ashar, zone), 62)
                        CalendarCell(PrayerCalculator.formatTime(t.maghrib, zone), 70)
                        CalendarCell(PrayerCalculator.formatTime(t.isya, zone), 62)
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.CalendarCell(
    text: String,
    widthDp: Int,
    header: Boolean = false
) {
    Text(
        text = text,
        color = if (header) Color.White else MaterialTheme.colorScheme.onSurface,
        fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier =
            Modifier
                .width(widthDp.dp)
                .padding(vertical = 6.dp, horizontal = 2.dp)
    )
}
