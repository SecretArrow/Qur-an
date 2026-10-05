package id.secretarrow.alquran.data.local

import android.content.Context
import id.secretarrow.alquran.data.model.SurahInfo
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Sumber data Al-Qur'an offline dari aset JSON yang di-bundle di APK.
 * Setiap edisi dimuat lazy, sekali saja, lalu disimpan di memori.
 */
class QuranLocalDataSource(
    private val context: Context
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    private val uthmani = AtomicReference<List<List<String>>?>(null)
    private val indopak = AtomicReference<List<List<String>>?>(null)
    private val transliteration = AtomicReference<List<List<String>>?>(null)
    private val kemenag = AtomicReference<List<List<String>>?>(null)
    private val jalalayn = AtomicReference<List<List<String>>?>(null)

    private val surahInfoRef = AtomicReference<List<SurahInfo>?>(null)
    private val mutex = Mutex()

    suspend fun surahInfo(): List<SurahInfo> {
        surahInfoRef.get()?.let { return it }
        return mutex.withLock {
            surahInfoRef.get() ?: run {
                val loaded =
                    withContext(Dispatchers.IO) {
                        json.decodeFromString<List<SurahInfo>>(readAsset("data/surah_info.json"))
                    }
                surahInfoRef.set(loaded)
                loaded
            }
        }
    }

    suspend fun surah(number: Int): SurahInfo = surahInfo()[number - 1]

    suspend fun ayahText(
        rasm: RasmEdition,
        surah: Int,
        ayah: Int
    ): String? = editionTexts(rasm).getOrNull(surah - 1)?.getOrNull(ayah - 1)

    suspend fun ayahList(
        rasm: RasmEdition,
        surah: Int
    ): List<String> = editionTexts(rasm).getOrNull(surah - 1) ?: emptyList()

    suspend fun editionTexts(rasm: RasmEdition): List<List<String>> {
        val cached: List<List<String>>? = refFor(rasm).get()
        if (cached != null) return cached
        return mutex.withLock {
            refFor(rasm).get() ?: run {
                val loaded =
                    withContext(Dispatchers.IO) {
                        json.decodeFromString<List<List<String>>>(readAsset(rasm.fileName))
                    }
                refFor(rasm).set(loaded)
                loaded
            }
        }
    }

    private fun refFor(rasm: RasmEdition): AtomicReference<List<List<String>>?> =
        when (rasm) {
            RasmEdition.UTHMANI -> uthmani
            RasmEdition.INDOPAK -> indopak
            RasmEdition.TRANSLITERATION -> transliteration
            RasmEdition.KEMENAG -> kemenag
            RasmEdition.JALALAYN -> jalalayn
        }

    private fun readAsset(path: String): String =
        context.assets
            .open(path)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
}

enum class RasmEdition(
    val fileName: String
) {
    UTHMANI("data/quran_uthmani.json"),
    INDOPAK("data/quran_indopak.json"),
    TRANSLITERATION("data/transliteration.json"),
    KEMENAG("data/translation_kemenag.json"),
    JALALAYN("data/translation_jalalayn.json")
}
