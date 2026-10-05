package id.secretarrow.alquran.ui.surahlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import id.secretarrow.alquran.data.model.Bookmark
import id.secretarrow.alquran.data.model.JuzEntry
import id.secretarrow.alquran.data.model.SurahInfo
import id.secretarrow.alquran.data.model.ThemeMode
import id.secretarrow.alquran.di.AppContainer
import id.secretarrow.alquran.ui.components.LogoBadge
import id.secretarrow.alquran.ui.components.StarBadge
import id.secretarrow.alquran.ui.navigation.AppViewModelFactory
import id.secretarrow.alquran.ui.theme.AmiriQuranFamily
import id.secretarrow.alquran.ui.theme.Gold
import id.secretarrow.alquran.ui.theme.GoldBright
import id.secretarrow.alquran.ui.theme.GoldText
import id.secretarrow.alquran.ui.theme.GreenDark
import id.secretarrow.alquran.ui.theme.Teal

private val TABS = listOf("SURAH", "JUZ", "BOOKMARK")

/** Daftar Surah / Juz / Bookmark (tab, sesuai aplikasi referensi). */
@Composable
fun SurahListScreen(
    container: AppContainer,
    initialTab: Int = 0,
    darkMode: ThemeMode,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
    onOpenSurah: (Int, Int) -> Unit,
    onPlaySurah: (Int, Int, Int) -> Unit,
    onAbout: () -> Unit
) {
    val factory =
        remember(
            container
        ) { AppViewModelFactory(container) { SurahListViewModel(container.quranRepository, container.bookmarkRepository) } }
    val viewModel: SurahListViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var aboutOpen by remember { mutableStateOf(false) }

    val surahName: (Int) -> String = { n -> state.surahs.getOrNull(n - 1)?.name ?: "Surah $n" }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LogoBadge()
                        Text(
                            text = "  Al-Qur'an Indonesia",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { sortMenuOpen = true }) {
                        Icon(Icons.Filled.Sort, contentDescription = "Urutkan", tint = Color.White)
                    }
                    DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Urut Nomor Surah") },
                            onClick = {
                                viewModel.setSortByRevelation(false)
                                sortMenuOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Urut Turun Wahyu") },
                            onClick = {
                                viewModel.setSortByRevelation(true)
                                sortMenuOpen = false
                            }
                        )
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = Color.White)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(if (darkMode == ThemeMode.DARK) "Tema Terang" else "Tema Gelap") },
                            leadingIcon = {
                                Icon(
                                    if (darkMode == ThemeMode.DARK) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                onToggleTheme()
                                menuOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Tentang Aplikasi") },
                            leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                            onClick = {
                                aboutOpen = true
                                menuOpen = false
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenDark)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = GreenDark,
                contentColor = Color.White,
                indicator = { positions ->
                    Box(
                        Modifier
                            .tabIndicatorOffset(positions[selectedTab])
                            .height(3.dp)
                            .background(GoldBright)
                    )
                }
            ) {
                TABS.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedTab == index) GoldBright else Color.White.copy(alpha = 0.75f),
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            when {
                state.loading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Teal)
                    }
                state.error != null ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.error ?: "Terjadi kesalahan", color = MaterialTheme.colorScheme.error)
                    }
                else ->
                    when (selectedTab) {
                        0 -> SurahTab(state, onOpenSurah, onPlaySurah)
                        1 -> JuzTab(state.juzs, onOpenSurah)
                        else -> BookmarkTab(bookmarks, surahName, onOpenSurah)
                    }
            }
        }
    }

    if (aboutOpen) {
        AlertDialog(
            onDismissRequest = { aboutOpen = false },
            confirmButton = {
                TextButton(onClick = { aboutOpen = false }) { Text("Tutup") }
            },
            title = { Text("Al-Qur'an Indonesia") },
            text = {
                Text(
                    "Replika open source aplikasi Al-Qur'an Indonesia.\n" +
                        "Teks Qur'an: Tanzil (Utsmani & IndoPak).\n" +
                        "Terjemahan: Kemenag RI & Tafsir Al-Jalalain.\n" +
                        "Audio murattal: everyayah.com (8 qori).\n" +
                        "Berjalan sepenuhnya offline."
                )
            }
        )
    }
}

@Composable
private fun SurahTab(
    state: SurahListUiState,
    onOpenSurah: (Int, Int) -> Unit,
    onPlaySurah: (Int, Int, Int) -> Unit
) {
    val list = state.surahs
    val sorted =
        if (state.sortByRevelation) {
            list.sortedWith(compareBy({ it.type == "Madaniyah" }, { it.number }))
        } else {
            list
        }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(sorted, key = { it.number }) { surah ->
            SurahRow(
                surah = surah,
                dark = surah.number % 2 == 0,
                onClick = { onOpenSurah(surah.number, 1) },
                onPlay = { onPlaySurah(1, surah.number, surah.ayahCount) }
            )
        }
    }
}

@Composable
private fun SurahRow(
    surah: SurahInfo,
    dark: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    val rowBg =
        if (dark) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(rowBg)
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.padding(start = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            StarBadge(surah.number)
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
        ) {
            Text(
                text = surah.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${surah.type.uppercase()} | ${surah.ayahCount} AYAT",
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
                color = GoldText
            )
        }
        Text(
            text = surah.arabic,
            fontFamily = AmiriQuranFamily,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Right,
            modifier = Modifier.padding(end = 8.dp)
        )
        // strip kolom audio
        Box(
            modifier =
                Modifier
                    .width(52.dp)
                    .height(58.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.VolumeUp,
                contentDescription = "Putar ${surah.name}",
                tint = Teal,
                modifier =
                    Modifier
                        .size(26.dp)
                        .clickable(onClick = onPlay)
            )
        }
    }
}

@Composable
private fun JuzTab(
    juzs: List<JuzEntry>,
    onOpenSurah: (Int, Int) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(juzs, key = { it.number }) { juz ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (juz.number % 2 ==
                                1
                            ) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ).clickable { onOpenSurah(juz.startSurah, juz.startAyah) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StarBadge(juz.number, size = 34.dp)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = "Juz ${juz.number}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = juz.label,
                            fontSize = 12.sp,
                            color = GoldText
                        )
                    }
                }
                Text(
                    text = "Buka",
                    color = Teal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun BookmarkTab(
    bookmarks: List<Bookmark>,
    surahName: (Int) -> String,
    onOpenSurah: (Int, Int) -> Unit
) {
    if (bookmarks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(42.dp)
                )
                Text(
                    text = "Belum ada bookmark.\nTekan ayat lalu pilih \"Tambah ke Bookmark\".",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(bookmarks, key = { it.id }) { bm ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(if (bm.surah % 2 == 1) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onOpenSurah(bm.surah, bm.ayah) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StarBadge(bm.surah, size = 34.dp)
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        text = surahName(bm.surah),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Ayat ${bm.ayah}",
                        fontSize = 12.sp,
                        color = GoldText
                    )
                }
                Text(
                    text = "${bm.surah}:${bm.ayah}",
                    color = Teal,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
