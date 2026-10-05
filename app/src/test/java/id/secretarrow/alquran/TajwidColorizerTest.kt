package id.secretarrow.alquran

import id.secretarrow.alquran.core.TajwidColorizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TajwidColorizerTest {
    @Test
    fun `teks kosong menghasilkan rentang kosong`() {
        assertTrue(TajwidColorizer.analyze("").isEmpty())
    }

    @Test
    fun `nun sukun sebelum huruf ikhfa diwarnai merah`() {
        // مِنْ تَحْتِهِ -> nun sukun + ta (huruf ikhfa)
        val text = "مِنْ تَحْتِهِ"
        val ranges = TajwidColorizer.analyze(text)
        val ikhfa = ranges.filter { it.rule == TajwidColorizer.Rule.IKHFA }
        assertTrue("harus ada aturan ikhfa, dapat: $ranges", ikhfa.isNotEmpty())
    }

    @Test
    fun `nun sukun sebelum ba adalah iqlab`() {
        // مِنْ بَعْدِ -> nun sukun + ba (iqlab)
        val text = "مِنْ بَعْدِ"
        val ranges = TajwidColorizer.analyze(text)
        assertTrue(
            "harus ada iqlab, dapat: $ranges",
            ranges.any { it.rule == TajwidColorizer.Rule.IQLAB }
        )
    }

    @Test
    fun `nun sukun sebelum ya diwarnai idgham bighunnah`() {
        // مَنْ يَعْمَلْ -> nun + ya
        val text = "مَنْ يَعْمَلْ"
        val ranges = TajwidColorizer.analyze(text)
        assertTrue(
            "harus ada idgham bighunnah, dapat: $ranges",
            ranges.any { it.rule == TajwidColorizer.Rule.IDGHAM_BIGHUNNAH }
        )
    }

    @Test
    fun `nun sebelum ba adalah iqlab`() {
        // مِنْ بَعْدِiqlab test pakai contoh lain: أَنْ بِدَالً -> tidak umum; pakai مِنْ بَعْدِ tetap ikhfa
        // contoh iqlab: مِنۢ بَعْدِ (tanwin + ba)
        val text = "مِنۢ بَعْدِ"
        val ranges = TajwidColorizer.analyze(text)
        // tanween kasr pada miim + ba -> iqlab
        assertTrue(
            "harus ada iqlab atau ikhfa pada tanween+ba, dapat: $ranges",
            ranges.any { it.rule == TajwidColorizer.Rule.IQLAB }
        )
    }

    @Test
    fun `mad thabi terdeteksi pada fathah plus alef`() {
        // قَالَ -> qaaf fathah + alef
        val text = "قَالَ"
        val ranges = TajwidColorizer.analyze(text)
        assertTrue("harus ada mad, dapat: $ranges", ranges.any { it.rule == TajwidColorizer.Rule.MAD })
    }

    @Test
    fun `qalqalah sukun terdeteksi`() {
        // أَقْرَأْ -> qaaf sukun
        val text = "أَقْرَأْ"
        val ranges = TajwidColorizer.analyze(text)
        assertTrue(
            "harus ada qalqalah, dapat: $ranges",
            ranges.any { it.rule == TajwidColorizer.Rule.QALQALAH }
        )
    }

    @Test
    fun `rentang tidak melewati batas teks`() {
        val text = "مِنْ بَعْدِ"
        val ranges = TajwidColorizer.analyze(text)
        ranges.forEach {
            assertTrue(it.start >= 0)
            assertTrue(it.end <= text.length)
            assertTrue(it.start < it.end)
        }
    }

    @Test
    fun `izhar tidak diberi aturan lain`() {
        // مَنْ هَادٍ -> nun + ha (izhar) tidak boleh berubah ikhfa/idgham/iqlab
        val text = "مَنْ هَادٍ"
        val ranges = TajwidColorizer.analyze(text)
        val nunRange = ranges.firstOrNull { it.start == text.indexOf('ن') }
        if (nunRange != null) {
            assertEquals(TajwidColorizer.Rule.IZHAR, nunRange.rule)
        }
    }
}
