package id.secretarrow.alquran.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.secretarrow.alquran.data.local.ReciterCatalog
import id.secretarrow.alquran.data.model.RasmStyle
import id.secretarrow.alquran.data.model.RepeatMode
import id.secretarrow.alquran.data.model.ThemeMode
import id.secretarrow.alquran.data.model.Translator
import id.secretarrow.alquran.di.AppContainer
import id.secretarrow.alquran.ui.navigation.AppViewModelFactory
import id.secretarrow.alquran.ui.theme.GreenDark
import id.secretarrow.alquran.ui.theme.Teal
import kotlinx.coroutines.launch

/** Pengaturan: Arabic, Latin, Terjemahan, Tema, Audio, Hijriah, Backup. */
@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val factory =
        remember(container) {
            AppViewModelFactory(container) { SettingsViewModel(container.settingsRepository, container.bookmarkRepository) }
        }
    val viewModel: SettingsViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri != null) {
                scope.launch {
                    val backup = viewModel.buildBackup()
                    if (backup != null) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(backup.toByteArray(Charsets.UTF_8))
                        }
                        viewModel.sendMessage("Backup tersimpan")
                    }
                }
            }
        }
    val importLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                scope.launch {
                    val text =
                        runCatching {
                            context.contentResolver
                                .openInputStream(uri)
                                ?.bufferedReader()
                                ?.use { it.readText() }
                        }.getOrNull()
                    if (text != null) {
                        viewModel.restoreBackup(text)
                    } else {
                        viewModel.sendMessage("Gagal membaca file backup")
                    }
                }
            }
        }

    val message = state.message
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    var rasmDialog by remember { mutableStateOf(false) }
    var translatorDialog by remember { mutableStateOf(false) }
    var themeDialog by remember { mutableStateOf(false) }
    var reciterDialog by remember { mutableStateOf(false) }
    var repeatDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                title = { Text("Pengaturan", color = Color.White, style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenDark)
            )
        }
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
        ) {
            SectionTitle("Arabic")
            SettingEntry(
                title = "Jenis Penulisan Arabic",
                subtitle = if (state.rasm == RasmStyle.INDOPAK) "IndoPak (Asia)" else "Utsmani (Standar)",
                onClick = { rasmDialog = true }
            )
            SettingEntry(
                title = "Ukuran Font Arabic",
                subtitle = "${state.arabicFontSize} px",
                onClick = { },
                trailing = {
                    Slider(
                        value = state.arabicFontSize.toFloat(),
                        onValueChange = { viewModel.setArabicFontSize(it.toInt()) },
                        valueRange = 14f..42f,
                        steps = 27,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                }
            )
            SettingSwitch(
                title = "Nomor Ayat Arabic",
                subtitle = "Perlihatkan nomor ayat arabic di ayat",
                checked = state.showArabicAyahNumber,
                onChange = { viewModel.setShowArabicAyahNumber(it) }
            )

            SectionTitle("Latin (Transliterasi)")
            SettingSwitch(
                title = "Aktifkan Latin",
                subtitle = "Perlihatkan latin (translitesi) Qur'an",
                checked = state.latinEnabled,
                onChange = { viewModel.setLatinEnabled(it) }
            )
            SettingEntry(
                title = "Ukuran Font Latin",
                subtitle = "${state.latinFontSize} px",
                onClick = { },
                trailing = {
                    Slider(
                        value = state.latinFontSize.toFloat(),
                        onValueChange = { viewModel.setLatinFontSize(it.toInt()) },
                        valueRange = 12f..30f,
                        steps = 17,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                }
            )

            SectionTitle("Terjemahan")
            SettingSwitch(
                title = "Aktifkan Terjemahan",
                subtitle = "Perlihatkan terjemahan Qur'an Bahasa Indonesia",
                checked = state.translationEnabled,
                onChange = { viewModel.setTranslationEnabled(it) }
            )
            SettingEntry(
                title = "Penerjemah",
                subtitle = if (state.translator == Translator.JALALAYN) "Tafsir Al Jalalain Indonesia" else "Kemenag-RI",
                onClick = { translatorDialog = true }
            )

            SectionTitle("Tampilan")
            SettingEntry(
                title = "Tema",
                subtitle =
                    when (state.darkMode) {
                        ThemeMode.SYSTEM -> "Ikuti Sistem"
                        ThemeMode.LIGHT -> "Terang"
                        ThemeMode.DARK -> "Gelap"
                    },
                onClick = { themeDialog = true }
            )
            SettingSwitch(
                title = "Jaga Layar Menyala",
                subtitle = "Layar tidak mati saat membaca",
                checked = state.keepScreenOn,
                onChange = { viewModel.setKeepScreenOn(it) }
            )

            SectionTitle("Audio Murattal")
            SettingEntry(
                title = "Pilihan Qori",
                subtitle = ReciterCatalog.byId(state.reciterId).name,
                onClick = { reciterDialog = true }
            )
            SettingEntry(
                title = "Mode Ulang",
                subtitle =
                    when (state.repeatMode) {
                        RepeatMode.OFF -> "Tanpa Ulang"
                        RepeatMode.ONE -> "Ulang Satu Ayat"
                        RepeatMode.ALL -> "Ulang Semua Ayat"
                    },
                onClick = { repeatDialog = true }
            )

            SectionTitle("Kalender Hijriah")
            SettingEntry(
                title = "Koreksi Tanggal Hijriah",
                subtitle =
                    if (state.hijriOffset ==
                        0
                    ) {
                        "Default (Umm al-Qura)"
                    } else {
                        "${if (state.hijriOffset > 0) "+" else ""}${state.hijriOffset} hari"
                    },
                onClick = { },
                trailing = {
                    Slider(
                        value = state.hijriOffset.toFloat(),
                        onValueChange = { viewModel.setHijriOffset(it.toInt()) },
                        valueRange = -2f..2f,
                        steps = 3,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                }
            )

            SectionTitle("Data")
            SettingEntry(
                title = "Backup Bookmark & Terakhir Baca",
                subtitle = "Simpan ke file JSON",
                onClick = { exportLauncher.launch("alquran-backup.json") }
            )
            SettingEntry(
                title = "Restore Bookmark",
                subtitle = "Pilih file backup JSON",
                onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) }
            )

            SectionTitle("Tentang")
            Text(
                text =
                    "Al-Qur'an Indonesia (replika open source)\n" +
                        "Teks: Tanzil (Utsmani & IndoPak) • Terjemahan: Kemenag-RI & Tafsir Al-Jalalain\n" +
                        "Audio: everyayah.com • Waktu sholat: konvensi Kemenag",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    if (rasmDialog) {
        RadioDialog(
            title = "Jenis Penulisan Arabic",
            options = listOf("Utsmani (Standar)" to RasmStyle.UTHMANI, "IndoPak (Asia)" to RasmStyle.INDOPAK),
            selected = state.rasm,
            onSelect = {
                viewModel.setRasm(it)
                rasmDialog = false
            },
            onDismiss = { rasmDialog = false }
        )
    }
    if (translatorDialog) {
        RadioDialog(
            title = "Penerjemah",
            options = listOf("Kemenag-RI" to Translator.KEMENAG, "Tafsir Al Jalalain Indonesia" to Translator.JALALAYN),
            selected = state.translator,
            onSelect = {
                viewModel.setTranslator(it)
                translatorDialog = false
            },
            onDismiss = { translatorDialog = false }
        )
    }
    if (themeDialog) {
        RadioDialog(
            title = "Tema",
            options = listOf("Ikuti Sistem" to ThemeMode.SYSTEM, "Terang" to ThemeMode.LIGHT, "Gelap" to ThemeMode.DARK),
            selected = state.darkMode,
            onSelect = {
                viewModel.setDarkMode(it)
                themeDialog = false
            },
            onDismiss = { themeDialog = false }
        )
    }
    if (reciterDialog) {
        RadioDialog(
            title = "Pilih Qori",
            options = ReciterCatalog.reciters.map { it.name to it.id },
            selected = state.reciterId,
            onSelect = {
                viewModel.setReciterId(it)
                reciterDialog = false
            },
            onDismiss = { reciterDialog = false }
        )
    }
    if (repeatDialog) {
        RadioDialog(
            title = "Mode Ulang",
            options = listOf("Tanpa Ulang" to RepeatMode.OFF, "Ulang Satu Ayat" to RepeatMode.ONE, "Ulang Semua Ayat" to RepeatMode.ALL),
            selected = state.repeatMode,
            onSelect = {
                viewModel.setRepeatMode(it)
                repeatDialog = false
            },
            onDismiss = { repeatDialog = false }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Teal,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        fontSize = 14.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun SettingEntry(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (trailing != null) {
            Box(modifier = Modifier.padding(top = 4.dp)) { trailing() }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange, thumbContent = null)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
private fun <T> RadioDialog(
    title: String,
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (label, value) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(value) }
                                .padding(vertical = 6.dp)
                    ) {
                        RadioButton(selected = value == selected, onClick = { onSelect(value) })
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}
