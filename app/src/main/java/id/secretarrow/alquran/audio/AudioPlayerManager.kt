package id.secretarrow.alquran.audio

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import id.secretarrow.alquran.data.local.Reciter
import id.secretarrow.alquran.data.local.ReciterCatalog
import id.secretarrow.alquran.data.model.RepeatMode
import id.secretarrow.alquran.data.repo.SettingsRepository
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Manager audio berbasis MediaController (tersambung ke [PlaybackService]).
 * Playlist = satu surah, satu MediaItem per ayat.
 */
class AudioPlayerManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val downloadManager: AudioDownloadManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val executor = Executors.newSingleThreadExecutor()

    private var controller: MediaController? = null
    private var started = false

    data class PlaybackState(
        val isPlaying: Boolean = false,
        val isPrepared: Boolean = false,
        val surah: Int = 0,
        val ayah: Int = 0,
        val repeatMode: RepeatMode = RepeatMode.OFF,
        val error: String? = null
    )

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state

    /** Akses state unduhan audio untuk UI. */
    fun downloadState(): StateFlow<AudioDownloadManager.DownloadState> = downloadManager.state

    /** Akses manajer unduhan untuk aksi unduh/hapus. */
    fun downloadManager(): AudioDownloadManager = downloadManager

    fun ensureController(ready: () -> Unit = {}) {
        if (controller != null) {
            ready()
            return
        }
        if (started) return
        started = true
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            try {
                val c = future.get()
                c.addListener(
                    object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _state.value = _state.value.copy(isPlaying = isPlaying)
                        }

                        override fun onMediaItemTransition(
                            mediaItem: MediaItem?,
                            reason: Int
                        ) {
                            val id = mediaItem?.mediaId
                            if (id != null) {
                                val parts = id.split(":")
                                if (parts.size == 2) {
                                    _state.value =
                                        _state.value.copy(
                                            surah = parts[0].toIntOrNull() ?: 0,
                                            ayah = parts[1].toIntOrNull() ?: 0
                                        )
                                }
                            }
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            _state.value = _state.value.copy(isPrepared = playbackState != Player.STATE_IDLE)
                            if (playbackState == Player.STATE_ENDED) {
                                _state.value = _state.value.copy(isPlaying = false)
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            _state.value =
                                _state.value.copy(
                                    isPlaying = false,
                                    error = "Gagal memutar audio. Periksa koneksi internet atau unduh audio lebih dulu."
                                )
                        }
                    }
                )
                controller = c
                _state.value =
                    _state.value.copy(
                        repeatMode = RepeatMode.entries.firstOrNull { it.name == repeatToName(c.repeatMode) } ?: RepeatMode.OFF
                    )
                ready()
            } catch (_: Exception) {
                started = false
            }
        }, executor)
    }

    /** Mainkan satu surah mulai ayat tertentu. */
    fun playSurah(
        reciter: Reciter,
        surah: Int,
        ayahCount: Int,
        startAyah: Int
    ) {
        ensureController {
            val c = controller ?: return@ensureController
            val items =
                (1..ayahCount).map { ayah ->
                    val mediaId = "$surah:$ayah"
                    val uri =
                        downloadManager.localUri(reciter, surah, ayah)
                            ?: android.net.Uri.parse(ReciterCatalog.ayahUrl(reciter, surah, ayah))
                    MediaItem
                        .Builder()
                        .setMediaId(mediaId)
                        .setUri(uri)
                        .setMediaMetadata(
                            MediaMetadata
                                .Builder()
                                .setTitle("Ayat $ayah")
                                .setArtist(reciter.name)
                                .build()
                        ).build()
                }
            c.setMediaItems(items, (startAyah - 1).coerceIn(0, ayahCount - 1), 0L)
            c.prepare()
            c.play()
        }
    }

    fun togglePlayPause() {
        ensureController {
            controller?.let { if (it.isPlaying) it.pause() else it.play() }
        }
    }

    fun stop() {
        ensureController {
            controller?.stop()
        }
    }

    fun next() {
        ensureController { controller?.seekToNextMediaItem() }
    }

    fun previous() {
        ensureController { controller?.seekToPreviousMediaItem() }
    }

    fun seekToAyah(
        surah: Int,
        ayah: Int
    ) {
        ensureController {
            val c = controller ?: return@ensureController
            val count = c.mediaItemCount
            for (i in 0 until count) {
                val item = c.getMediaItemAt(i)
                if (item.mediaId == "$surah:$ayah") {
                    c.seekTo(i, 0L)
                    if (!c.isPlaying) c.play()
                    return@ensureController
                }
            }
        }
    }

    fun setRepeatMode(mode: RepeatMode) {
        ensureController {
            controller?.repeatMode =
                when (mode) {
                    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                }
            _state.value = _state.value.copy(repeatMode = mode)
        }
    }

    private fun repeatToName(mode: Int): String =
        when (mode) {
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE.name
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL.name
            else -> RepeatMode.OFF.name
        }

    /** Sinkronkan repeat mode dari preferensi saat aplikasi dibuka. */
    fun syncRepeatModeWithSettings() {
        scope.launch {
            val mode = settingsRepository.settings.first().repeatMode
            setRepeatMode(mode)
        }
    }

    fun release() {
        controller?.release()
        controller = null
        scope.cancel()
    }
}
