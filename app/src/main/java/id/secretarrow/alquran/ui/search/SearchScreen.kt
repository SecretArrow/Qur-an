package id.secretarrow.alquran.ui.search

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.secretarrow.alquran.data.repo.SearchNormalizer
import id.secretarrow.alquran.di.AppContainer
import id.secretarrow.alquran.ui.components.StarBadge
import id.secretarrow.alquran.ui.navigation.AppViewModelFactory
import id.secretarrow.alquran.ui.theme.GoldText
import id.secretarrow.alquran.ui.theme.GreenDark
import id.secretarrow.alquran.ui.theme.OrnamentBg
import id.secretarrow.alquran.ui.theme.RedTajwid
import id.secretarrow.alquran.ui.theme.Teal

/** Pencarian ayat berdasarkan terjemahan (Mendekati / Terperinci). */
@Composable
fun SearchScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenAyah: (Int, Int) -> Unit
) {
    val factory =
        remember(container) {
            AppViewModelFactory(container) { SearchViewModel(container.quranRepository) }
        }
    val viewModel: SearchViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                        }
                    },
                    title = {
                        Text("Pencarian", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenDark)
                )
                Column(
                    modifier =
                        Modifier
                            .background(GreenDark)
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp)
                ) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = { viewModel.setQuery(it) },
                        placeholder = { Text("Masukkan kata kunci...", color = Color.White.copy(alpha = 0.6f)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White) },
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setQuery("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Bersihkan", tint = Color.White)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.White.copy(alpha = 0.12f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.12f)
                            ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                    ) {
                        SegmentedButton(
                            selected = !state.exactWord,
                            onClick = { viewModel.setExactWord(false) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) { Text("Mendekati") }
                        SegmentedButton(
                            selected = state.exactWord,
                            onClick = { viewModel.setExactWord(true) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) { Text("Terperinci") }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
        ) {
            LaunchedEffect(state.query) {
                if (state.query.isBlank()) return@LaunchedEffect
                kotlinx.coroutines.delay(350)
                viewModel.search()
            }
            when {
                state.searching ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Teal)
                    }
                state.query.isBlank() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Ketik kata kunci untuk mencari ayat\ndalam terjemahan Al-Qur'an",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                state.searched && state.hits.isEmpty() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Tidak ditemukan hasil untuk \"${state.query}\"",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                else -> SearchResults(state, viewModel, onOpenAyah)
            }
        }
    }
}

@Composable
private fun SearchResults(
    state: SearchUiState,
    viewModel: SearchViewModel,
    onOpenAyah: (Int, Int) -> Unit
) {
    val grouped = state.hits.groupBy { it.surah }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        grouped.forEach { (surah, hits) ->
            val expanded = surah.number in state.expandedSurahs
            item(key = "header_${surah.number}") {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(OrnamentBg)
                            .clickable { viewModel.toggleExpanded(surah.number) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "[${surah.number}]. QS. ${surah.name} ",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "(ditemukan ${hits.size} ayat)",
                        color = GoldText,
                        fontSize = 13.sp
                    )
                    Box(Modifier.weight(1f))
                    Icon(
                        if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expanded) "Tutup" else "Buka",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (expanded) {
                items(hits, key = { "s${surah.number}_a${it.ayah}" }) { hit ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { onOpenAyah(surah.number, hit.ayah) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        StarBadge(hit.ayah, size = 30.dp)
                        Text(
                            text = highlightText(hit.translation, state.query),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp)
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }
    }
}

/** Sorot kata yang cocok dengan warna merah (mode terperinci = kata utuh). */
fun highlightText(
    text: String,
    query: String
): androidx.compose.ui.text.AnnotatedString {
    val q = SearchNormalizer.normalize(query)
    if (q.isBlank()) return buildAnnotatedString { append(text) }
    val norm = SearchNormalizer.normalize(text)
    if (!norm.contains(q)) return buildAnnotatedString { append(text) }
    return buildAnnotatedString {
        // pemetaan kasar: sorot kemunculan kata pada teks asli secara case-insensitive
        val lower = text.lowercase()
        var index = 0
        val tokens = q.split(" ").filter { it.isNotBlank() }
        val primary = tokens.firstOrNull() ?: return@buildAnnotatedString
        while (index < lower.length) {
            val found = lower.indexOf(primary, index)
            if (found == -1) {
                append(text.substring(index))
                break
            }
            append(text.substring(index, found))
            withStyle(SpanStyle(color = RedTajwid, fontWeight = FontWeight.Bold)) {
                append(text.substring(found, (found + primary.length).coerceAtMost(text.length)))
            }
            index = found + primary.length
        }
    }
}
