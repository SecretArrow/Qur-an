package id.secretarrow.alquran.ui.reader

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.secretarrow.alquran.core.ArabicUtils
import id.secretarrow.alquran.core.TajwidColorizer
import id.secretarrow.alquran.data.model.AyahDisplay
import id.secretarrow.alquran.ui.components.OrnamentHeader
import id.secretarrow.alquran.ui.components.StarBadge
import id.secretarrow.alquran.ui.theme.AmberTajwid
import id.secretarrow.alquran.ui.theme.AmiriQuranFamily
import id.secretarrow.alquran.ui.theme.GreenTajwid
import id.secretarrow.alquran.ui.theme.GreenTajwidSoft
import id.secretarrow.alquran.ui.theme.NotoNaskhFamily
import id.secretarrow.alquran.ui.theme.OrangeTajwid
import id.secretarrow.alquran.ui.theme.PurpleTajwid
import id.secretarrow.alquran.ui.theme.RedTajwid
import id.secretarrow.alquran.ui.theme.Teal

private const val BASMALAH = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"

/** Satu halaman surah di dalam pager. */
@Composable
fun ReaderPage(
    surahNumber: Int,
    state: ReaderUiState,
    viewModel: ReaderViewModel,
    isInitial: Boolean,
    initialAyah: Int,
    onAyahClick: (Int) -> Unit
) {
    val page = state.pages[surahNumber]
    val listState = rememberLazyListState()

    if (page == null) {
        // trigger load & tampilkan loading
        LaunchedEffect(surahNumber) { viewModel.loadPage(surahNumber) }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Teal)
        }
        return
    }

    // Gulir ke ayat awal bila dibuka dari bookmark/terakhir baca/juz
    if (isInitial && initialAyah > 1) {
        LaunchedEffect(initialAyah) {
            listState.scrollToItem(index = initialAyah, scrollOffset = -80)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val info = page.surah
        OrnamentHeader(
            leftLabel = info.type,
            centerLabel = info.meaning,
            rightLabel = "${info.ayahCount}\nAyat"
        )
        if (info.number != 1 && info.number != 9) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = BASMALAH,
                    fontFamily =
                        if (state.rasm ==
                            id.secretarrow.alquran.data.model.RasmStyle.INDOPAK
                        ) {
                            NotoNaskhFamily
                        } else {
                            AmiriQuranFamily
                        },
                    fontSize = (state.arabicFontSize + 6).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
        AyahList(
            ayahs = page.ayahs,
            state = state,
            listState = listState,
            playingAyah = if (state.playingSurah == surahNumber) state.playingAyah else 0,
            bookmarks = state.bookmarksBySurah[surahNumber] ?: emptySet(),
            onAyahClick = onAyahClick,
            onPlay = { ayah -> viewModel.playAyahInSurah(surahNumber, ayah) }
        )
    }
}

@Composable
private fun AyahList(
    ayahs: List<AyahDisplay>,
    state: ReaderUiState,
    listState: LazyListState,
    playingAyah: Int,
    bookmarks: Set<Int>,
    onAyahClick: (Int) -> Unit,
    onPlay: (Int) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(ayahs, key = { _, a -> a.ayahNumber }) { _, ayah ->
            AyahRow(
                ayah = ayah,
                state = state,
                highlighted = playingAyah == ayah.ayahNumber,
                isBookmarked = ayah.ayahNumber in bookmarks,
                onClick = { onAyahClick(ayah.ayahNumber) },
                onPlay = { onPlay(ayah.ayahNumber) }
            )
        }
        // ruang bawah agar tak tertutup audio bar
        item { Box(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun AyahRow(
    ayah: AyahDisplay,
    state: ReaderUiState,
    highlighted: Boolean,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    val bg =
        when {
            highlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            ayah.ayahNumber % 2 == 1 -> MaterialTheme.colorScheme.surface
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(bg)
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        // Baris 1: badge kiri, teks Arab kanan
        Row(verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.padding(top = 4.dp)) {
                StarBadge(ayah.ayahNumber, size = 32.dp)
            }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                val arabicFont =
                    if (state.rasm == id.secretarrow.alquran.data.model.RasmStyle.INDOPAK) {
                        NotoNaskhFamily
                    } else {
                        AmiriQuranFamily
                    }
                val marker =
                    if (state.showArabicAyahNumber) {
                        " " + ArabicUtils.toArabicNumber(ayah.ayahNumber) + "۝"
                    } else {
                        ""
                    }
                Text(
                    text = tajwidAnnotated(ayah.arabic + marker),
                    fontFamily = arabicFont,
                    fontSize = state.arabicFontSize.sp,
                    lineHeight = (state.arabicFontSize * 2).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Right
                )
            }
        }
        // Baris 2: tombol play + latin
        if (state.latinEnabled && ayah.latin.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Putar ayat ${ayah.ayahNumber}",
                    tint = Teal,
                    modifier =
                        Modifier
                            .size(26.dp)
                            .clickable(onClick = onPlay)
                )
                Text(
                    text = ayah.latin,
                    color = Teal,
                    fontSize = state.latinFontSize.sp,
                    lineHeight = (state.latinFontSize * 1.5f).sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
        // Baris 3: terjemahan
        if (state.translationEnabled && ayah.translation.isNotBlank()) {
            Text(
                text = ayah.translation,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = (state.latinFontSize + 1).sp,
                lineHeight = ((state.latinFontSize + 1) * 1.55f).sp,
                modifier = Modifier.padding(top = 4.dp, start = 2.dp)
            )
        }
        if (isBookmarked) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    Icons.Filled.Bookmark,
                    contentDescription = "Sudah dibookmark",
                    tint = id.secretarrow.alquran.ui.theme.Gold,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "  Dibookmark",
                    fontSize = 11.sp,
                    color = id.secretarrow.alquran.ui.theme.Gold
                )
            }
        }
    }
}

/** Bangun AnnotatedString dengan warna tajwid. */
fun tajwidAnnotated(text: String): AnnotatedString {
    val ranges = TajwidColorizer.analyze(text)
    return buildAnnotatedString {
        append(text)
        ranges.forEach { range ->
            val color =
                when (range.rule) {
                    TajwidColorizer.Rule.IKHFA -> RedTajwid
                    TajwidColorizer.Rule.IDGHAM_BIGHUNNAH -> GreenTajwid
                    TajwidColorizer.Rule.IDGHAM_BILAGHUNNAH -> GreenTajwidSoft
                    TajwidColorizer.Rule.IQLAB -> PurpleTajwid
                    TajwidColorizer.Rule.IZHAR -> AmberTajwid
                    TajwidColorizer.Rule.QALQALAH -> OrangeTajwid
                    TajwidColorizer.Rule.MAD -> AmberTajwid
                }
            addStyle(SpanStyle(color = color), range.start, range.end)
        }
    }
}

/** Bottom sheet aksi ayat: Play, Bagikan, Salin, Bookmark, Tandai Terakhir Baca. */
@Composable
fun AyahActionSheet(
    surahName: String,
    surah: Int,
    ayah: Int,
    isBookmarked: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onBookmark: (Boolean) -> Unit,
    onMarkLastRead: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val shareText =
        remember(surah, ayah) {
            buildString {
                append("QS. ")
                    .append(surahName)
                    .append(": ")
                    .append(surah)
                    .append(':')
                    .append(ayah)
                append("\n(Dari Al-Qur'an Indonesia)")
            }
        }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "QS. $surahName: Ayat $ayah",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        SheetItem(
            icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
            label = "Play Murattal",
            onClick = onPlay
        )
        SheetItem(
            icon = { Icon(Icons.Filled.Share, contentDescription = null) },
            label = "Bagikan Ayat",
            onClick = {
                val intent =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                context.startActivity(Intent.createChooser(intent, "Bagikan ayat"))
                onShare()
            }
        )
        SheetItem(
            icon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
            label = "Salin Ayat",
            onClick = {
                clipboard.setText(AnnotatedString(shareText))
                onCopy()
            }
        )
        SheetItem(
            icon = {
                Icon(
                    if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = null
                )
            },
            label = if (isBookmarked) "Hapus dari Bookmark" else "Tambah ke Bookmark",
            onClick = { onBookmark(!isBookmarked) }
        )
        SheetItem(
            icon = { Icon(Icons.Filled.PushPin, contentDescription = null) },
            label = "Tandai Terakhir Baca",
            onClick = onMarkLastRead
        )
        TextButton(
            onClick = onDismiss,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
        ) {
            Text("Batal")
        }
    }
}

@Composable
private fun SheetItem(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 14.dp)
        )
    }
}
