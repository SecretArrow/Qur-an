package id.secretarrow.alquran.audio

import android.content.Context
import id.secretarrow.alquran.data.local.Reciter
import id.secretarrow.alquran.data.local.ReciterCatalog
import java.io.File
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manajer unduhan audio per-ayat: file disimpan di external files dir,
 * bisa diunduh satu ayat/satu surah dan dihapus lagi.
 */
class AudioDownloadManager(
    private val context: Context,
    private val scope: kotlinx.coroutines.CoroutineScope
) {
    data class DownloadState(
        val activeCount: Int = 0,
        val lastMessage: String? = null
    )

    private val _state = MutableStateFlow(DownloadState())
    val state: StateFlow<DownloadState> = _state

    private fun dirFor(reciter: Reciter): File {
        val dir = File(context.getExternalFilesDir(null), "audio/" + reciter.folder)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun fileFor(
        reciter: Reciter,
        surah: Int,
        ayah: Int
    ): File = File(dirFor(reciter), "%03d%03d.mp3".format(surah, ayah))

    fun isDownloaded(
        reciter: Reciter,
        surah: Int,
        ayah: Int
    ): Boolean = fileFor(reciter, surah, ayah).exists() && fileFor(reciter, surah, ayah).length() > 0

    /** URI lokal bila sudah terunduh, selain itu null (pakai streaming). */
    fun localUri(
        reciter: Reciter,
        surah: Int,
        ayah: Int
    ): android.net.Uri? {
        val f = fileFor(reciter, surah, ayah)
        return if (f.exists() && f.length() > 0) android.net.Uri.fromFile(f) else null
    }

    fun downloadedCount(
        reciter: Reciter,
        surah: Int,
        ayahCount: Int
    ): Int = (1..ayahCount).count { isDownloaded(reciter, surah, it) }

    fun downloadSurah(
        reciter: Reciter,
        surah: Int,
        ayahCount: Int
    ) {
        val targets = (1..ayahCount).filter { !isDownloaded(reciter, surah, it) }
        if (targets.isEmpty()) {
            _state.value = _state.value.copy(lastMessage = "Audio surah $surah sudah lengkap terunduh")
            return
        }
        targets.forEach { ayah ->
            scope.launch {
                _state.value = _state.value.copy(activeCount = _state.value.activeCount + 1)
                val ok =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val url = URL(ReciterCatalog.ayahUrl(reciter, surah, ayah))
                            val tmp = File(dirFor(reciter), "%03d%03d.mp3.part".format(surah, ayah))
                            url.openStream().use { input -> tmp.outputStream().use { output -> input.copyTo(output) } }
                            if (tmp.length() > 0) {
                                tmp.renameTo(fileFor(reciter, surah, ayah))
                            } else {
                                tmp.delete()
                                error("empty download")
                            }
                        }.isSuccess
                    }
                _state.value =
                    _state.value.copy(
                        activeCount = (_state.value.activeCount - 1).coerceAtLeast(0),
                        lastMessage = if (ok) "Ayat $surah:$ayah terunduh" else "Gagal mengunduh $surah:$ayah"
                    )
            }
        }
    }

    fun deleteSurah(
        reciter: Reciter,
        surah: Int,
        ayahCount: Int
    ) {
        (1..ayahCount).forEach { ayah ->
            fileFor(reciter, surah, ayah).delete()
        }
        _state.value = _state.value.copy(lastMessage = "Audio surah $surah dihapus")
    }
}
