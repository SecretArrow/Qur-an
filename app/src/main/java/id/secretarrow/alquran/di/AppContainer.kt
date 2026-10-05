package id.secretarrow.alquran.di

import android.content.Context
import androidx.room.Room
import id.secretarrow.alquran.audio.AudioDownloadManager
import id.secretarrow.alquran.audio.AudioPlayerManager
import id.secretarrow.alquran.data.db.AppDatabase
import id.secretarrow.alquran.data.local.QuranLocalDataSource
import id.secretarrow.alquran.data.repo.BookmarkRepository
import id.secretarrow.alquran.data.repo.QuranRepository
import id.secretarrow.alquran.data.repo.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Kontainer dependency sederhana (tanpa framework DI agar build tetap cepat). */
class AppContainer(
    context: Context
) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val appContext: Context = context.applicationContext

    val database: AppDatabase by lazy {
        Room
            .databaseBuilder(appContext, AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()
    }

    val quranLocalDataSource: QuranLocalDataSource by lazy { QuranLocalDataSource(appContext) }
    val quranRepository: QuranRepository by lazy { QuranRepository(quranLocalDataSource) }
    val bookmarkRepository: BookmarkRepository by lazy { BookmarkRepository(database) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val audioDownloadManager: AudioDownloadManager by lazy { AudioDownloadManager(appContext, appScope) }
    val audioPlayerManager: AudioPlayerManager by lazy {
        AudioPlayerManager(appContext, settingsRepository, audioDownloadManager)
    }

    /** Cache nama surah untuk tampilan menu (dimuat sekali). */
    val surahNameCache: StateFlow<Map<Int, String>> by lazy {
        val flow = MutableStateFlow<Map<Int, String>>(emptyMap())
        appScope.launch {
            flow.value = quranRepository.surahList().associate { it.number to it.name }
        }
        flow
    }

    init {
        audioPlayerManager.syncRepeatModeWithSettings()
    }
}
