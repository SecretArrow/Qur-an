package id.secretarrow.alquran.data.repo

import id.secretarrow.alquran.data.local.QuranLocalDataSource
import id.secretarrow.alquran.data.local.RasmEdition
import id.secretarrow.alquran.data.model.AyahDisplay
import id.secretarrow.alquran.data.model.JuzEntry
import id.secretarrow.alquran.data.model.RasmStyle
import id.secretarrow.alquran.data.model.SurahInfo
import id.secretarrow.alquran.data.model.Translator

/**
 * Repository utama Al-Qur'an: menggabungkan teks Arab, latin, terjemahan,
 * dan metadata surah menjadi tampilan siap render.
 */
class QuranRepository(
    private val local: QuranLocalDataSource
) {
    suspend fun surahList(): List<SurahInfo> = local.surahInfo()

    suspend fun surah(number: Int): SurahInfo = local.surah(number)

    suspend fun ayahs(
        surah: Int,
        rasm: RasmStyle,
        latinEnabled: Boolean,
        translationEnabled: Boolean,
        translator: Translator
    ): List<AyahDisplay> {
        val arabicEdition = if (rasm == RasmStyle.INDOPAK) RasmEdition.INDOPAK else RasmEdition.UTHMANI
        val arabicList = local.ayahList(arabicEdition, surah)
        val latinList = if (latinEnabled) local.ayahList(RasmEdition.TRANSLITERATION, surah) else emptyList()
        val translationEdition = if (translator == Translator.JALALAYN) RasmEdition.JALALAYN else RasmEdition.KEMENAG
        val translationList =
            if (translationEnabled) {
                local.ayahList(translationEdition, surah)
            } else {
                emptyList()
            }
        val count = maxOf(arabicList.size, translationList.size, latinList.size)
        return (1..count).map { ayah ->
            AyahDisplay(
                surahNumber = surah,
                ayahNumber = ayah,
                arabic = arabicList.getOrNull(ayah - 1).orEmpty(),
                latin = latinList.getOrNull(ayah - 1).orEmpty(),
                translation = translationList.getOrNull(ayah - 1).orEmpty()
            )
        }
    }

    /** Cari terjemahan berdasarkan kata kunci; kembalikan hit per surah. */
    suspend fun searchTranslation(
        query: String,
        exactWord: Boolean,
        limitPerSurah: Int = Int.MAX_VALUE
    ): List<Pair<SurahInfo, List<Pair<Int, String>>>> {
        val normalizedQuery = SearchNormalizer.normalize(query)
        if (normalizedQuery.isBlank()) return emptyList()
        val results = mutableListOf<Pair<SurahInfo, List<Pair<Int, String>>>>()
        val translations = local.editionTexts(RasmEdition.KEMENAG)
        val surahs = local.surahInfo()
        translations.forEachIndexed { index, ayahs ->
            val hits =
                ayahs.mapIndexedNotNull { ayahIndex, text ->
                    val norm = SearchNormalizer.normalize(text)
                    val matched =
                        if (exactWord) {
                            SearchNormalizer.matchesWholeWord(norm, normalizedQuery)
                        } else {
                            norm.contains(normalizedQuery)
                        }
                    if (matched) ayahIndex + 1 to text else null
                }
            if (hits.isNotEmpty()) {
                val limited = hits.take(limitPerSurah)
                results.add(surahs[index] to limited)
            }
        }
        return results
    }

    /** Daftar 30 juz berdasarkan batas juz standar. */
    suspend fun juzEntries(): List<JuzEntry> {
        val surahs = local.surahInfo()
        val juzStarts =
            listOf(
                1 to 1,
                2 to 142,
                2 to 253,
                3 to 93,
                4 to 24,
                4 to 148,
                5 to 82,
                6 to 111,
                7 to 88,
                8 to 41,
                9 to 93,
                11 to 6,
                12 to 53,
                15 to 1,
                17 to 1,
                18 to 75,
                21 to 1,
                23 to 1,
                25 to 21,
                27 to 56,
                29 to 46,
                33 to 31,
                36 to 28,
                39 to 32,
                41 to 47,
                46 to 1,
                51 to 31,
                58 to 1,
                67 to 1,
                78 to 1
            )
        val surahStart = surahs.associateBy({ it.number }, { it.startAyahGlobal })
        val surahEnd = surahs.associateBy({ it.number }, { it.startAyahGlobal + it.ayahCount - 1 })
        return juzStarts.mapIndexed { idx, (s, a) ->
            val nextStart =
                if (idx + 1 < juzStarts.size) {
                    val (ns, na) = juzStarts[idx + 1]
                    surahStart.getValue(ns) + na - 1
                } else {
                    surahEnd.getValue(114)
                }
            // Surah akhir juz = surah yang memuat ayah global (nextStart - 1)
            val endSurah = surahs.last { surahEnd.getValue(it.number) <= nextStart - 1 }.number
            val endAyah = (nextStart - 1) - surahStart.getValue(endSurah) + 1
            JuzEntry(
                number = idx + 1,
                startSurah = s,
                startAyah = a,
                endSurah = endSurah,
                endAyah = endAyah,
                label = "${surahs[s - 1].name} $s:$a – ${surahs[endSurah - 1].name} $endSurah:$endAyah"
            )
        }
    }
}

/** Normalisasi teks untuk pencarian. */
object SearchNormalizer {
    private val punctuation = Regex("[^\\p{L}\\p{Nd}\\s]")

    fun normalize(input: String): String =
        punctuation
            .replace(input.lowercase(), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun matchesWholeWord(
        normalizedText: String,
        normalizedQuery: String
    ): Boolean {
        if (normalizedQuery.isBlank()) return false
        return normalizedText.split(" ").any { it == normalizedQuery }
    }
}
