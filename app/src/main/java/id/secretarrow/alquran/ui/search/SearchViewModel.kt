package id.secretarrow.alquran.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.secretarrow.alquran.data.model.SurahInfo
import id.secretarrow.alquran.data.repo.QuranRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchHit(
    val surah: SurahInfo,
    val ayah: Int,
    val translation: String
)

data class SearchUiState(
    val query: String = "",
    val exactWord: Boolean = false,
    val searching: Boolean = false,
    val expandedSurahs: Set<Int> = emptySet(),
    val hits: List<SearchHit> = emptyList(),
    val searched: Boolean = false
)

class SearchViewModel(
    private val quranRepository: QuranRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    fun setQuery(q: String) {
        _state.value = _state.value.copy(query = q)
    }

    fun setExactWord(exact: Boolean) {
        _state.value = _state.value.copy(exactWord = exact)
        search()
    }

    fun toggleExpanded(surah: Int) {
        val current = _state.value.expandedSurahs
        _state.value =
            _state.value.copy(
                expandedSurahs = if (surah in current) current - surah else current + surah
            )
    }

    fun search() {
        val query = _state.value.query.trim()
        if (query.isEmpty()) {
            _state.value = _state.value.copy(hits = emptyList(), searched = false, searching = false)
            return
        }
        val exact = _state.value.exactWord
        viewModelScope.launch {
            _state.value = _state.value.copy(searching = true)
            try {
                val groups = quranRepository.searchTranslation(query, exact)
                val hits =
                    groups.flatMap { (surah, ayahHits) ->
                        ayahHits.map { (ayah, text) -> SearchHit(surah, ayah, text) }
                    }
                // Perluas surah pertama otomatis (mengikuti perilaku referensi)
                val firstExpanded =
                    hits
                        .firstOrNull()
                        ?.surah
                        ?.number
                        ?.let { setOf(it) } ?: emptySet()
                _state.value = _state.value.copy(searching = false, hits = hits, searched = true, expandedSurahs = firstExpanded)
            } catch (e: Exception) {
                _state.value = _state.value.copy(searching = false, hits = emptyList(), searched = true)
            }
        }
    }
}
