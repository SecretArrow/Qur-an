package id.secretarrow.alquran.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.secretarrow.alquran.core.BackupCodec
import id.secretarrow.alquran.data.model.RasmStyle
import id.secretarrow.alquran.data.model.RepeatMode
import id.secretarrow.alquran.data.model.ThemeMode
import id.secretarrow.alquran.data.model.Translator
import id.secretarrow.alquran.data.repo.BookmarkRepository
import id.secretarrow.alquran.data.repo.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val loading: Boolean = true,
    val rasm: RasmStyle = RasmStyle.UTHMANI,
    val arabicFontSize: Int = 20,
    val showArabicAyahNumber: Boolean = true,
    val latinEnabled: Boolean = true,
    val latinFontSize: Int = 16,
    val translationEnabled: Boolean = true,
    val translator: Translator = Translator.KEMENAG,
    val darkMode: ThemeMode = ThemeMode.SYSTEM,
    val reciterId: Int = 1,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val adzanEnabled: Boolean = true,
    val hijriOffset: Int = 0,
    val keepScreenOn: Boolean = false,
    val message: String? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                _state.value =
                    _state.value.copy(
                        loading = false,
                        rasm = s.rasm,
                        arabicFontSize = s.arabicFontSize,
                        showArabicAyahNumber = s.showArabicAyahNumber,
                        latinEnabled = s.latinEnabled,
                        latinFontSize = s.latinFontSize,
                        translationEnabled = s.translationEnabled,
                        translator = s.translator,
                        darkMode = s.darkMode,
                        reciterId = s.reciterId,
                        repeatMode = s.repeatMode,
                        adzanEnabled = s.adzanEnabled,
                        hijriOffset = s.hijriOffset,
                        keepScreenOn = s.keepScreenOn
                    )
            }
        }
    }

    fun setRasm(v: RasmStyle) = launchSetting { settingsRepository.setRasm(v) }

    fun setArabicFontSize(v: Int) = launchSetting { settingsRepository.setArabicFontSize(v) }

    fun setShowArabicAyahNumber(v: Boolean) = launchSetting { settingsRepository.setShowArabicAyahNumber(v) }

    fun setLatinEnabled(v: Boolean) = launchSetting { settingsRepository.setLatinEnabled(v) }

    fun setLatinFontSize(v: Int) = launchSetting { settingsRepository.setLatinFontSize(v) }

    fun setTranslationEnabled(v: Boolean) = launchSetting { settingsRepository.setTranslationEnabled(v) }

    fun setTranslator(v: Translator) = launchSetting { settingsRepository.setTranslator(v) }

    fun setDarkMode(v: ThemeMode) = launchSetting { settingsRepository.setDarkMode(v) }

    fun setReciterId(v: Int) = launchSetting { settingsRepository.setReciterId(v) }

    fun setRepeatMode(v: RepeatMode) = launchSetting { settingsRepository.setRepeatMode(v) }

    fun setAdzanEnabled(v: Boolean) = launchSetting { settingsRepository.setAdzanEnabled(v) }

    fun setHijriOffset(v: Int) = launchSetting { settingsRepository.setHijriOffset(v) }

    fun setKeepScreenOn(v: Boolean) = launchSetting { settingsRepository.setKeepScreenOn(v) }

    private fun launchSetting(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    /** Buat string backup untuk disimpan pengguna via SAF. */
    suspend fun buildBackup(): String? =
        try {
            val bookmarks = bookmarkRepository.all()
            val lastRead = bookmarkRepository.currentLastRead()
            BackupCodec.encode(bookmarks, lastRead, System.currentTimeMillis())
        } catch (e: Exception) {
            _state.value = _state.value.copy(message = "Gagal membuat backup: ${e.message}")
            null
        }

    /** Restore dari isi file backup. */
    fun restoreBackup(text: String) {
        viewModelScope.launch {
            BackupCodec
                .decode(text)
                .onSuccess { (bookmarks, lastRead) ->
                    bookmarkRepository.clearAll()
                    bookmarkRepository.restore(bookmarks)
                    bookmarkRepository.restoreLastRead(lastRead)
                    _state.value = _state.value.copy(message = "Restore berhasil: ${bookmarks.size} bookmark")
                }.onFailure {
                    _state.value = _state.value.copy(message = "File backup tidak valid")
                }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun sendMessage(text: String) {
        _state.value = _state.value.copy(message = text)
    }

    class Factory(
        private val container: id.secretarrow.alquran.di.AppContainer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(container.settingsRepository, container.bookmarkRepository) as T
    }
}
