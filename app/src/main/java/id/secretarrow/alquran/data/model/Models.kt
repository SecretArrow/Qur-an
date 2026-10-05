package id.secretarrow.alquran.data.model

import kotlinx.serialization.Serializable

/** Metadata satu surah (114 surah). */
@Serializable
data class SurahInfo(
    val number: Int,
    val name: String,
    val arabic: String,
    val meaning: String,
    val type: String,
    val ayahCount: Int,
    /** Nomor juz tempat surah ini dimulai. */
    val juzStart: Int = 1,
    /** Ayah global awal (basis komputasi juz). */
    val startAyahGlobal: Int = 1
)

/** Satu ayat lengkap untuk tampilan. */
data class AyahDisplay(
    val surahNumber: Int,
    val ayahNumber: Int,
    val arabic: String,
    val latin: String,
    val translation: String,
    val isPlaying: Boolean = false,
    val isBookmarked: Boolean = false
)

/** Bookmark ayat. */
data class Bookmark(
    val id: Long = 0L,
    val surah: Int,
    val ayah: Int,
    val createdAt: Long
)

/** Penanda bacaan terakhir (baris tunggal, id selalu 1). */
data class LastRead(
    val surah: Int,
    val ayah: Int,
    val updatedAt: Long
)

/** Entri juz untuk indeks Juz. */
data class JuzEntry(
    val number: Int,
    val startSurah: Int,
    val startAyah: Int,
    val endSurah: Int,
    val endAyah: Int,
    val label: String
)

/** Waktu sholat hari ini (epoch millis). */
data class PrayerTimesOfDay(
    val imsak: Long,
    val subuh: Long,
    val terbit: Long,
    val dzuhur: Long,
    val ashar: Long,
    val maghrib: Long,
    val isya: Long
)

/** Toggle alarm per waktu sholat. */
data class AdzanToggles(
    val imsak: Boolean = false,
    val subuh: Boolean = true,
    val terbit: Boolean = false,
    val dzuhur: Boolean = true,
    val ashar: Boolean = true,
    val maghrib: Boolean = true,
    val isya: Boolean = true
)

enum class RasmStyle { UTHMANI, INDOPAK }

enum class Translator { KEMENAG, JALALAYN }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class RepeatMode { OFF, ONE, ALL }

/** Nama waktu untuk jadwal sholat & alarm. */
enum class PrayerName { IMSAK, SUBUH, TERBIT, DZUHUR, ASHAR, MAGHRIB, ISYA }
