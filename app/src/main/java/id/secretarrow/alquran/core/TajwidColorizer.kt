package id.secretarrow.alquran.core

/**
 * Penghasil rentang warna tajwid untuk teks Arab Uthmani.
 * Aturan disederhanakan namun nyata: ikhfa, idgham, iqlab, izhar, qalqalah, mad.
 */
object TajwidColorizer {
    enum class Rule { IKHFA, IDGHAM_BIGHUNNAH, IDGHAM_BILAGHUNNAH, IQLAB, IZHAR, QALQALAH, MAD }

    data class Range(
        val start: Int,
        val end: Int,
        val rule: Rule
    )

    // U+064B..U+0652: tanween fath, dam, kasr, syadda, sukun
    private fun isSukun(cp: Int): Boolean = cp == 0x0652 || cp == 0x06E1 || cp == 0x065C

    private fun isFathah(cp: Int): Boolean = cp == 0x064E

    private fun isTanween(cp: Int): Boolean = cp in 0x064B..0x064D

    private fun isNunLetter(cp: Int): Boolean = cp == 0x0646 // ن

    private fun isBaLetter(cp: Int): Boolean = cp == 0x0628 // ب

    // huruf ikhfa (15): ت ث ج د ذ ز س ش ص ض ط ظ ف ق ك
    private val ikhfaSet =
        intArrayOf(0x062A, 0x062B, 0x062C, 0x062F, 0x0630, 0x0632, 0x0633, 0x0634, 0x0635, 0x0636, 0x0637, 0x0638, 0x0641, 0x0642, 0x0643)

    // idgham bighunnah: ي ن م و
    private val idghamBigSet = intArrayOf(0x064A, 0x0646, 0x0645, 0x0648)

    // idgham bilaghunnah: ل ر
    private val idghamSmallSet = intArrayOf(0x0644, 0x0631)

    // izhar: ء ه ع ح غ خ
    private val izharSet = intArrayOf(0x0621, 0x0647, 0x0639, 0x062D, 0x063A, 0x062E)

    // qalqalah: ق ط ب ج د
    private val qalqalahSet = intArrayOf(0x0642, 0x0637, 0x0628, 0x062C, 0x062F)

    private fun inSet(
        cp: Int,
        set: IntArray
    ): Boolean = set.any { it == cp }

    /**
     * Analisis teks Arab (Uthmani) dan kembalikan rentang karakter [start, end) yang berlaku
     * untuk tiap aturan tajwid. Rentang meng-cover base letter + harakatnya.
     */
    fun analyze(text: String): List<Range> {
        val ranges = mutableListOf<Range>()
        val chars = text.codePoints().toArray()
        val n = chars.size
        var i = 0

        // indeks base letter (bukan harakat) berikut
        fun nextBaseIndex(from: Int): Int {
            var j = from
            while (j < n) {
                val cp = chars[j]
                val isMark = cp in 0x064B..0x065F || cp in 0x06D6..0x06ED || cp == 0x0670
                if (!isMark && cp != 0x20 && cp != 0x09 && cp != 0x0A) return j
                j++
            }
            return -1
        }

        fun baseEnd(from: Int): Int {
            var j = from + 1
            while (j < n) {
                val cp = chars[j]
                val isMark = cp in 0x064B..0x065F || cp in 0x06D6..0x06ED || cp == 0x0670
                if (!isMark) return j
                j++
            }
            return j
        }

        while (i < n) {
            val cp = chars[i]

            // Aturan nun sukun / tanween pada base sebelumnya
            val isNunSukun =
                isNunLetter(cp) &&
                    run {
                        var j = i + 1
                        var hasSukun = false
                        while (j < n && isMarkCp(chars[j])) {
                            if (isSukun(chars[j])) hasSukun = true
                            j++
                        }
                        hasSukun
                    }
            val prevMarked =
                i > 0 &&
                    run {
                        // tanween pada base sebelumnya, atau nunsukun
                        var j = i - 1
                        var hasTanween = false
                        while (j >= 0 && isMarkCp(chars[j])) {
                            if (isTanween(chars[j])) hasTanween = true
                            j--
                        }
                        hasTanween
                    }

            if (isNunSukun || prevMarked) {
                val nextBase = nextBaseIndex(i + 1)
                if (nextBase != -1) {
                    val next = chars[nextBase]
                    val end = baseEnd(nextBase)
                    val rule =
                        when {
                            inSet(next, idghamBigSet) -> Rule.IDGHAM_BIGHUNNAH
                            inSet(next, idghamSmallSet) -> Rule.IDGHAM_BILAGHUNNAH
                            isBaLetter(next) -> Rule.IQLAB
                            inSet(next, ikhfaSet) -> Rule.IKHFA
                            inSet(next, izharSet) -> Rule.IZHAR
                            else -> null
                        }
                    if (rule != null) ranges.add(Range(i, end, rule))
                }
            }

            // Qalqalah: base qalqalah + sukun
            if (inSet(cp, qalqalahSet)) {
                var j = i + 1
                var hasSukun = false
                while (j < n && isMarkCp(chars[j])) {
                    if (isSukun(chars[j])) hasSukun = true
                    j++
                }
                if (hasSukun) ranges.add(Range(i, j, Rule.QALQALAH))
            }

            // Mad thabi'i: fathah diikuti alef
            if (i + 1 < n) {
                var j = i + 1
                var hadFathah = false
                while (j < n && isMarkCp(chars[j])) {
                    if (isFathah(chars[j])) hadFathah = true
                    j++
                }
                if (hadFathah && j < n && chars[j] == 0x0627) {
                    ranges.add(Range(i, j + 1, Rule.MAD))
                }
            }

            i++
        }
        return ranges
    }

    private fun isMarkCp(cp: Int): Boolean = cp in 0x064B..0x065F || cp in 0x06D6..0x06ED || cp == 0x0670
}
