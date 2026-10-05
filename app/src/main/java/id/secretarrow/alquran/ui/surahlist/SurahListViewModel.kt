package id.secretarrow.alquran.ui.surahlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.secretarrow.alquran.data.local.ReciterCatalog
import id.secretarrow.alquran.data.model.Bookmark
import id.secretarrow.alquran.data.model.JuzEntry
import id.secretarrow.alquran.data.model.SurahInfo
import id.secretarrow.alquran.data.repo.BookmarkRepository
import id.secretarrow.alquran.data.repo.QuranRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SurahListUiState(
    val loading: Boolean = true,
    val surahs: List<SurahInfo> = emptyList(),
    val juzs: List<JuzEntry> = emptyList(),
    val error: String? = null,
    val sortByRevelation: Boolean = false
)

class SurahListViewModel(
    quranRepository: QuranRepository,
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SurahListUiState())
    val state: StateFlow<SurahListUiState> = _state.asStateFlow()

    val bookmarks: StateFlow<List<Bookmark>> =
        bookmarkRepository
            .bookmarks()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            try {
                val surahs = quranRepository.surahList()
                val juzs = quranRepository.juzEntries()
                _state.value = _state.value.copy(loading = false, surahs = surahs, juzs = juzs)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Gagal memuat daftar surah")
            }
        }
    }

    fun setSortByRevelation(enabled: Boolean) {
        _state.value = _state.value.copy(sortByRevelation = enabled)
    }

    fun sortedSurahs(): List<SurahInfo> {
        val s = _state.value.surahs
        return if (_state.value.sortByRevelation) {
            s.sortedWith(compareBy({ it.type == "Madaniyah" }, { it.number }))
        } else {
            s
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch { bookmarkRepository.toggle(bookmark.surah, bookmark.ayah) }
    }

    fun playSurah(
        reciterId: Int,
        surah: Int,
        ayahCount: Int,
        onPlay: (Int, Int, Int) -> Unit
    ) {
        onPlay(reciterId, surah, ayahCount)
        ReciterCatalog.byId(reciterId)
    }
}
