package id.secretarrow.alquran

import id.secretarrow.alquran.core.ArabicUtils
import id.secretarrow.alquran.core.BackupCodec
import id.secretarrow.alquran.core.HijriHelper
import id.secretarrow.alquran.core.QiblaUtils
import id.secretarrow.alquran.data.local.Reciter
import id.secretarrow.alquran.data.local.ReciterCatalog
import id.secretarrow.alquran.data.model.Bookmark
import id.secretarrow.alquran.data.model.LastRead
import id.secretarrow.alquran.data.repo.SearchNormalizer
import id.secretarrow.alquran.prayer.PrayerCalculator
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicUtilsTest {
    @Test
    fun `angka dikonversi ke arabic-indic`() {
        assertEquals("١", ArabicUtils.toArabicNumber(1))
        assertEquals("٢٥٥", ArabicUtils.toArabicNumber(255))
        assertEquals("١١٤", ArabicUtils.toArabicNumber(114))
    }
}

class ReciterCatalogTest {
    @Test
    fun `tersedia 8 qori`() {
        assertEquals(8, ReciterCatalog.reciters.size)
    }

    @Test
    fun `url per ayat format tiga digit`() {
        val reciter = Reciter(1, "Test", "Alafasy_128kbps")
        assertEquals(
            "https://everyayah.com/data/Alafasy_128kbps/002255.mp3",
            ReciterCatalog.ayahUrl(reciter, 2, 255)
        )
        assertTrue(ReciterCatalog.ayahUrl(reciter, 1, 1).endsWith("001001.mp3"))
    }

    @Test
    fun `byId fallback ke qori pertama bila tidak ada`() {
        assertEquals(ReciterCatalog.reciters.first(), ReciterCatalog.byId(999))
    }
}

class QiblaUtilsTest {
    @Test
    fun `arah kiblat dari Jakarta sekitar 295 derajat`() {
        // Sesuai screenshot referensi: "Qiblat 295.22° dari Utara"
        val angle = QiblaUtils.qiblaDirection(-6.2088, 106.8456)
        assertTrue("kiblat Jakarta harus ~295°, dapat $angle", angle in 293.0..297.0)
    }

    @Test
    fun `jarak Ka bah dari Jakarta sekitar 7900 km`() {
        val km = QiblaUtils.distanceToKaabaKm(-6.2088, 106.8456)
        assertTrue("jarak harus ~7916 km, dapat $km", km in 7700.0..8100.0)
    }

    @Test
    fun `dari Ka bah sudut ke dirinya sendiri stabil`() {
        val angle = QiblaUtils.qiblaDirection(QiblaUtils.KAABA_LAT, QiblaUtils.KAABA_LNG)
        assertTrue(angle in 0.0..360.0)
    }
}

class HijriHelperTest {
    @Test
    fun `format hijriah memuat tahun dan H`() {
        val result = HijriHelper.format(LocalDate.of(2022, 3, 26))
        assertTrue(result.endsWith("H"))
        assertTrue(result.contains("1443"))
    }

    @Test
    fun `nama hari dan masehi indonesia`() {
        val date = LocalDate.of(2022, 3, 26) // Sabtu
        assertEquals("Sabtu", HijriHelper.dayName(date))
        assertEquals("26 Maret 2022", HijriHelper.formatGregorian(date))
    }
}

class SearchNormalizerTest {
    @Test
    fun `normalisasi lowercase dan buang tanda baca`() {
        assertEquals("allah yang maha pengasih", SearchNormalizer.normalize("Allah, Yang Maha Pengasih!"))
    }

    @Test
    fun `kata utuh hanya cocok pada token penuh`() {
        val text = SearchNormalizer.normalize("Segala puji bagi Allah, Tuhan seluruh alam")
        assertTrue(SearchNormalizer.matchesWholeWord(text, "allah"))
        assertFalse(SearchNormalizer.matchesWholeWord(text, "lah"))
    }
}

class BackupCodecTest {
    @Test
    fun `encode decode roundtrip`() {
        val bookmarks = listOf(Bookmark(surah = 2, ayah = 255, createdAt = 123L))
        val lastRead = LastRead(surah = 18, ayah = 10, updatedAt = 456L)
        val encoded = BackupCodec.encode(bookmarks, lastRead, now = 789L)
        val decoded = BackupCodec.decode(encoded).getOrThrow()
        assertEquals(1, decoded.first.size)
        assertEquals(2, decoded.first[0].surah)
        assertEquals(255, decoded.first[0].ayah)
        assertEquals(18, decoded.second?.surah)
        assertEquals(10, decoded.second?.ayah)
    }

    @Test
    fun `file rusak mengembalikan error bukan crash`() {
        val result = BackupCodec.decode("{bukan json")
        assertTrue(result.isFailure)
    }
}

class PrayerCalculatorTest {
    private val zone = ZoneId.of("Asia/Jakarta")

    @Test
    fun `waktu subuh dan isya wajar untuk Jakarta`() {
        val times = PrayerCalculator.calculate(-6.2088, 106.8456, LocalDate.of(2022, 3, 26))
        assertTrue("subuh harus sekitar 04:30-05:10", times.subuh > 0)
        val subuhTime =
            java.time.Instant
                .ofEpochMilli(times.subuh)
                .atZone(zone)
        assertTrue("jam subuh 3-6, dapat $subuhTime", subuhTime.hour in 3..5)
        val isyaTime =
            java.time.Instant
                .ofEpochMilli(times.isya)
                .atZone(zone)
        assertTrue("jam isya 18-20, dapat $isyaTime", isyaTime.hour in 18..20)
    }

    @Test
    fun `imsak adalah subuh dikurangi 10 menit`() {
        val times = PrayerCalculator.calculate(-6.2088, 106.8456, LocalDate.of(2022, 3, 26))
        assertEquals(times.subuh - 10 * 60_000L, times.imsak)
    }

    @Test
    fun `formatTime menghasilkan HH mm`() {
        val times = PrayerCalculator.calculate(-6.2088, 106.8456, LocalDate.of(2022, 3, 26))
        val formatted = PrayerCalculator.formatTime(times.dzuhur, zone)
        assertTrue(Regex("^\\d{2}:\\d{2}$").matches(formatted))
    }
}
