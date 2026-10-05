package id.secretarrow.alquran.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.secretarrow.alquran.audio.AudioPlayerManager
import id.secretarrow.alquran.data.local.ReciterCatalog
import id.secretarrow.alquran.data.model.AyahDisplay
import id.secretarrow.alquran.data.model.RasmStyle
import id.secretarrow.alquran.data.model.RepeatMode
import id.secretarrow.alquran.data.model.SurahInfo
import id.secretarrow.alquran.data.model.Translator
import id.secretarrow.alquran.data.repo.BookmarkRepository
import id.secretarrow.alquran.data.repo.QuranRepository
import id.secretarrow.alquran.data.repo.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Data satu halaman surah. */
data class ReaderPageData(
    val surah: SurahInfo,
    val ayahs: List<AyahDisplay>
)

/** State pembacaan (mendukung swipe antar surah via cache halaman). */
data class ReaderUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val pages: Map<Int, ReaderPageData> = emptyMap(),
    val rasm: RasmStyle = RasmStyle.UTHMANI,
    val arabicFontSize: Int = 20,
    val showArabicAyahNumber: Boolean = true,
    val latinEnabled: Boolean = true,
    val latinFontSize: Int = 16,
    val translationEnabled: Boolean = true,
    val translator: Translator = Translator.KEMENAG,
    val bookmarksBySurah: Map<Int, Set<Int>> = emptyMap(),
    val reciterId: Int = 1,
    val playingSurah: Int = 0,
    val playingAyah: Int = 0,
    val isPlaying: Boolean = false,
    val playbackError: String? = null,
    val downloadMessage: String? = null,
    val downloadedAyahCount: Int = 0
)

class ReaderViewModel(
    private val quranRepository: QuranRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val settingsRepository: SettingsRepository,
    private val audioPlayerManager: AudioPlayerManager
) : ViewModel() {
    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    private var initialSurah = 0
    private var lastReadMarked = false

    init {
        viewModelScope.launch {
            combine(
                audioPlayerManager.state,
                bookmarkRepository.bookmarks(),
                audioPlayerManager.downloadState()
            ) { playback, bookmarks, download ->
                Triple(playback, bookmarks, download)
            }.collect { (playback, bookmarks, download) ->
                _state.value =
                    _state.value.copy(
                        playingSurah = playback.surah,
                        playingAyah = playback.ayah,
                        isPlaying = playback.isPlaying,
                        playbackError = playback.error,
                        downloadMessage = download.lastMessage,
                        bookmarksBySurah =
                            bookmarks
                                .groupBy { it.surah }
                                .mapValues { entry -> entry.value.map { it.ayah }.toSet() }
                    )
            }
        }
    }

    /** Muat surah awal (dipanggil sekali saat layar dibuka). */
    fun load(
        surah: Int,
        startAyah: Int
    ) {
        initialSurah = surah
        loadPage(surah, markLastReadAyah = startAyah)
    }

    /** Muat satu halaman surah ke cache. */
    fun loadPage(
        surah: Int,
        markLastReadAyah: Int? = null
    ) {
        if (_state.value.pages.containsKey(surah)) return
        if (surah !in 1..114) {
            _state.value = _state.value.copy(loading = false, error = "Surah tidak valid")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val settings = settingsRepository.settings.first()
                val info = quranRepository.surah(surah)
                val ayahs =
                    quranRepository.ayahs(
                        surah = surah,
                        rasm = settings.rasm,
                        latinEnabled = settings.latinEnabled,
                        translationEnabled = settings.translationEnabled,
                        translator = settings.translator
                    )
                _state.value =
                    _state.value.copy(
                        loading = false,
                        error = null,
                        pages = _state.value.pages + (surah to ReaderPageData(info, ayahs)),
                        rasm = settings.rasm,
                        arabicFontSize = settings.arabicFontSize,
                        showArabicAyahNumber = settings.showArabicAyahNumber,
                        latinEnabled = settings.latinEnabled,
                        latinFontSize = settings.latinFontSize,
                        translationEnabled = settings.translationEnabled,
                        translator = settings.translator,
                        reciterId = settings.reciterId
                    )
                if (!lastReadMarked) {
                    lastReadMarked = true
                    bookmarkRepository.setLastRead(surah, markLastReadAyah ?: 1)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Gagal memuat surah")
            }
        }
    }

    /** Terapkan ulang pengaturan tampilan pada semua halaman ter-cache. */
    fun refreshWithSettings() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val newPages =
                _state.value.pages.mapValues { (_, page) ->
                    val ayahs =
                        quranRepository.ayahs(
                            surah = page.surah.number,
                            rasm = settings.rasm,
                            latinEnabled = settings.latinEnabled,
                            translationEnabled = settings.translationEnabled,
                            translator = settings.translator
                        )
                    ReaderPageData(page.surah, ayahs)
                }
            _state.value =
                _state.value.copy(
                    pages = newPages,
                    rasm = settings.rasm,
                    arabicFontSize = settings.arabicFontSize,
                    showArabicAyahNumber = settings.showArabicAyahNumber,
                    latinEnabled = settings.latinEnabled,
                    latinFontSize = settings.latinFontSize,
                    translationEnabled = settings.translationEnabled,
                    translator = settings.translator,
                    reciterId = settings.reciterId
                )
        }
    }

    fun pageFor(surah: Int): ReaderPageData? = _state.value.pages[surah]

    fun toggleBookmark(
        surah: Int,
        ayah: Int,
        onDone: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val added = bookmarkRepository.toggle(surah, ayah)
            onDone(added)
        }
    }

    fun markLastRead(
        surah: Int,
        ayah: Int
    ) {
        viewModelScope.launch { bookmarkRepository.setLastRead(surah, ayah) }
    }

    fun playSurah(
        surah: Int,
        startAyah: Int
    ) {
        val page = pageFor(surah) ?: return
        audioPlayerManager.ensureController {
            audioPlayerManager.playSurah(
                reciter = ReciterCatalog.byId(_state.value.reciterId),
                surah = surah,
                ayahCount = page.surah.ayahCount,
                startAyah = startAyah
            )
        }
    }

    fun playAyahInSurah(
        surah: Int,
        ayah: Int
    ) = playSurah(surah, ayah)

    fun togglePlayPause() = audioPlayerManager.togglePlayPause()

    fun stopPlayback() = audioPlayerManager.stop()

    fun nextAyah() = audioPlayerManager.next()

    fun previousAyah() = audioPlayerManager.previous()

    fun cycleRepeatMode() {
        viewModelScope.launch {
            val current = audioPlayerManager.state.value.repeatMode
            val nextMode =
                when (current) {
                    RepeatMode.OFF -> RepeatMode.ONE
                    RepeatMode.ONE -> RepeatMode.ALL
                    RepeatMode.ALL -> RepeatMode.OFF
                }
            audioPlayerManager.setRepeatMode(nextMode)
            settingsRepository.setRepeatMode(nextMode)
        }
    }

    fun repeatMode(): RepeatMode = audioPlayerManager.state.value.repeatMode

    fun setReciter(id: Int) {
        viewModelScope.launch { settingsRepository.setReciterId(id) }
        _state.value = _state.value.copy(reciterId = id)
    }

    fun downloadSurahAudio(surah: Int) {
        val page = pageFor(surah) ?: return
        audioPlayerManager
            .downloadManager()
            .downloadSurah(ReciterCatalog.byId(_state.value.reciterId), surah, page.surah.ayahCount)
    }

    fun deleteSurahAudio(surah: Int) {
        val page = pageFor(surah) ?: return
        audioPlayerManager
            .downloadManager()
            .deleteSurah(ReciterCatalog.byId(_state.value.reciterId), surah, page.surah.ayahCount)
    }

    fun downloadedCount(surah: Int): Int {
        val page = pageFor(surah) ?: return 0
        return audioPlayerManager
            .downloadManager()
            .downloadedCount(ReciterCatalog.byId(_state.value.reciterId), surah, page.surah.ayahCount)
    }

    fun clearPlaybackError() {
        _state.value = _state.value.copy(playbackError = null)
    }

    fun clearDownloadMessage() {
        _state.value = _state.value.copy(downloadMessage = null)
    }
}
