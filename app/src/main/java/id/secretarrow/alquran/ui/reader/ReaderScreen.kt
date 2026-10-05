package id.secretarrow.alquran.ui.reader

import android.app.Activity
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.secretarrow.alquran.data.local.ReciterCatalog.reciters
import id.secretarrow.alquran.data.model.RepeatMode
import id.secretarrow.alquran.data.model.RepeatMode.ALL
import id.secretarrow.alquran.data.model.RepeatMode.OFF
import id.secretarrow.alquran.data.model.RepeatMode.ONE
import id.secretarrow.alquran.di.AppContainer
import id.secretarrow.alquran.ui.components.LogoBadge
import id.secretarrow.alquran.ui.navigation.AppViewModelFactory
import id.secretarrow.alquran.ui.theme.GoldBright
import id.secretarrow.alquran.ui.theme.GreenDark
import id.secretarrow.alquran.ui.theme.Teal

/** Layar membaca: swipe antar-surah (RTL), tajwid berwarna, latin, terjemahan, audio. */
@Composable
fun ReaderScreen(
    container: AppContainer,
    initialSurah: Int,
    initialAyah: Int,
    onBack: () -> Unit,
    onOpenSurahList: () -> Unit,
    keepScreenOn: Boolean
) {
    val factory =
        remember(container) {
            AppViewModelFactory(container) {
                ReaderViewModel(
                    container.quranRepository,
                    container.bookmarkRepository,
                    container.settingsRepository,
                    container.audioPlayerManager
                )
            }
        }
    val viewModel: ReaderViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val pagerState =
        rememberPagerState(
            initialPage = (initialSurah - 1).coerceIn(0, 113),
            pageCount = { 114 }
        )

    // Muat halaman saat page berubah (termasuk tetangga agar swipe mulus)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val surah = page + 1
            viewModel.loadPage(surah)
            if (page + 1 <= 113) viewModel.loadPage(surah + 1)
            if (page - 1 >= 1) viewModel.loadPage(surah - 1)
        }
    }
    LaunchedEffect(initialSurah, initialAyah) {
        if (state.pages.isEmpty()) viewModel.load(initialSurah, initialAyah)
    }

    // Jaga layar tetap menyala sesuai pengaturan
    DisposableEffect(keepScreenOn) {
        val activity = context as? Activity
        if (keepScreenOn && activity != null) {
            activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Snackbar error / info
    val playbackError = state.playbackError
    LaunchedEffect(playbackError) {
        if (playbackError != null) {
            snackbarHostState.showSnackbar(playbackError)
            viewModel.clearPlaybackError()
        }
    }
    val downloadMessage = state.downloadMessage
    LaunchedEffect(downloadMessage) {
        if (downloadMessage != null) {
            snackbarHostState.showSnackbar(downloadMessage)
            viewModel.clearDownloadMessage()
        }
    }

    var menuOpen by remember { mutableStateOf(false) }
    var reciterDialog by remember { mutableStateOf(false) }
    var jumpDialog by remember { mutableStateOf(false) }
    var selectedActionAyah by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                            }
                            LogoBadge()
                        }
                    },
                    title = {
                        val currentSurah = pagerState.currentPage + 1
                        Text(
                            text = state.pages[currentSurah]?.surah?.name ?: "Surah $currentSurah",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        IconButton(onClick = onOpenSurahList) {
                            Icon(Icons.Filled.FormatListNumbered, contentDescription = "Daftar surah", tint = Color.White)
                        }
                        IconButton(onClick = { reciterDialog = true }) {
                            Icon(Icons.Filled.VolumeUp, contentDescription = "Pilih qori", tint = Color.White)
                        }
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = Color.White)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            val currentSurah = pagerState.currentPage + 1
                            DropdownMenuItem(
                                text = { Text("Pindah ke Ayat") },
                                onClick = {
                                    jumpDialog = true
                                    menuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Unduh Audio Surah Ini") },
                                onClick = {
                                    viewModel.downloadSurahAudio(currentSurah)
                                    menuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Hapus Audio Surah Ini") },
                                onClick = {
                                    viewModel.deleteSurahAudio(currentSurah)
                                    menuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Muat Ulang Pengaturan") },
                                onClick = {
                                    viewModel.refreshWithSettings()
                                    menuOpen = false
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenDark)
                )
                // strip pager: next | current | prev (RTL)
                PagerStrip(pagerState, state)
            }
        },
        bottomBar = {
            if (state.isPlaying || state.playingSurah != 0) {
                AudioBar(
                    isPlaying = state.isPlaying,
                    repeatLabel = viewModel.repeatMode(),
                    onToggleRepeat = { viewModel.cycleRepeatMode() },
                    onPrevious = { viewModel.previousAyah() },
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.nextAyah() },
                    onStop = { viewModel.stopPlayback() }
                )
            }
        }
    ) { padding ->
        if (state.error != null && state.pages.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error ?: "Terjadi kesalahan", color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { viewModel.load(initialSurah, initialAyah) }) {
                        Text("Coba lagi")
                    }
                }
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                reverseLayout = true,
                beyondViewportPageCount = 1
            ) { page ->
                val surahNumber = page + 1
                ReaderPage(
                    surahNumber = surahNumber,
                    state = state,
                    viewModel = viewModel,
                    isInitial = surahNumber == initialSurah,
                    initialAyah = initialAyah,
                    onAyahClick = { selectedActionAyah = it }
                )
            }
        }
    }

    if (reciterDialog) {
        ReciterDialog(
            selectedId = state.reciterId,
            onSelect = {
                viewModel.setReciter(it)
                reciterDialog = false
            },
            onDismiss = { reciterDialog = false }
        )
    }

    if (jumpDialog) {
        val currentSurah = pagerState.currentPage + 1
        val maxAyah = state.pages[currentSurah]?.surah?.ayahCount ?: 7
        JumpAyahDialog(
            maxAyah = maxAyah,
            onConfirm = { ayah ->
                jumpDialog = false
                if (ayah in 1..maxAyah) {
                    viewModel.playSurah(currentSurah, ayah)
                    viewModel.markLastRead(currentSurah, ayah)
                }
            },
            onDismiss = { jumpDialog = false }
        )
    }

    val actionAyah = selectedActionAyah
    if (actionAyah != null) {
        val currentSurah = pagerState.currentPage + 1
        AyahActionSheet(
            surahName = state.pages[currentSurah]?.surah?.name ?: "Surah $currentSurah",
            surah = currentSurah,
            ayah = actionAyah,
            isBookmarked = state.bookmarksBySurah[currentSurah]?.contains(actionAyah) == true,
            onDismiss = { selectedActionAyah = null },
            onPlay = {
                viewModel.playAyahInSurah(currentSurah, actionAyah)
                selectedActionAyah = null
            },
            onShare = { selectedActionAyah = null },
            onCopy = { selectedActionAyah = null },
            onBookmark = { added ->
                viewModel.toggleBookmark(currentSurah, actionAyah) {}
                selectedActionAyah = null
            },
            onMarkLastRead = {
                viewModel.markLastRead(currentSurah, actionAyah)
                selectedActionAyah = null
            }
        )
    }
}

/** Strip nama surah prev/current/next di bawah app bar (geser RTL). */
@Composable
private fun PagerStrip(
    pagerState: androidx.compose.foundation.pager.PagerState,
    state: ReaderUiState
) {
    val current = pagerState.currentPage + 1
    val prev = (current - 1).takeIf { it >= 1 }
    val next = (current + 1).takeIf { it <= 114 }

    fun label(n: Int?): String =
        if (n == null) {
            ""
        } else {
            state.pages[n]
                ?.surah
                ?.name
                ?.let { "$n. $it" } ?: "$n. Surah"
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(GreenDark)
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label(next),
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 13.sp,
            textAlign = TextAlign.Left,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label(current),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Box(
                modifier =
                    Modifier
                        .width(56.dp)
                        .height(3.dp)
                        .background(GoldBright)
            )
        }
        Text(
            text = label(prev),
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 13.sp,
            textAlign = TextAlign.Right,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
    }
}

@Composable
private fun AudioBar(
    isPlaying: Boolean,
    repeatLabel: RepeatMode,
    onToggleRepeat: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleRepeat) {
            when (repeatLabel) {
                ONE ->
                    Icon(Icons.Filled.RepeatOne, contentDescription = "Ulang satu ayat", tint = Teal)
                ALL ->
                    Icon(Icons.Filled.Repeat, contentDescription = "Ulang semua", tint = Teal)
                OFF ->
                    Icon(Icons.Filled.Repeat, contentDescription = "Ulang mati", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = onPrevious) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Ayat sebelumnya")
        }
        IconButton(onClick = onPlayPause) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Jeda" else "Putar",
                tint = Teal,
                modifier = Modifier.size(40.dp)
            )
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Ayat berikutnya")
        }
        IconButton(onClick = onStop) {
            Icon(Icons.Filled.Stop, contentDescription = "Berhenti")
        }
    }
}

@Composable
private fun ReciterDialog(
    selectedId: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val reciters = reciters
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        },
        title = { Text("Pilih Qori") },
        text = {
            Column {
                reciters.forEach { reciter ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(reciter.id) }
                                .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (reciter.id == selectedId) Icons.Filled.VolumeUp else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = if (reciter.id == selectedId) Teal else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = reciter.name,
                            modifier = Modifier.padding(start = 12.dp),
                            color = if (reciter.id == selectedId) Teal else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun JumpAyahDialog(
    maxAyah: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pindah ke Ayat (1–$maxAyah)") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() }.take(3) },
                label = { Text("Nomor ayat") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.toIntOrNull() ?: 0) }) { Text("Pindah") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
