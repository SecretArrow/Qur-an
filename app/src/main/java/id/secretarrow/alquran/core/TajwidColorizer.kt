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

    private fun isSukun(cp: Int): Boolean = cp == 0x0652 || cp == 0x06E1 || cp == 0x065C

    private fun isFathah(cp: Int): Boolean = cp == 0x064E

    private fun isTanween(cp: Int): Boolean = cp in 0x064B..0x064D

    private fun isMarkCp(cp: Int): Boolean = cp in 0x064B..0x065F || cp in 0x06D6..0x06ED || cp == 0x0670

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

    /** Apakah base letter di [i] adalah nun (ن) dengan sukun. */
    private fun isNunSukun(
        chars: IntArray,
        i: Int
    ): Boolean {
        if (chars[i] != 0x0646) return false
        var j = i + 1
        var found = false
        while (j < chars.size && isMarkCp(chars[j])) {
            if (isSukun(chars[j])) found = true
            j++
        }
        return found
    }

    /** Apakah base letter sebelum [i] membawa tanween. */
    private fun hasTanweenBefore(
        chars: IntArray,
        i: Int
    ): Boolean {
        var j = i - 1
        var found = false
        while (j >= 0 && isMarkCp(chars[j])) {
            if (isTanween(chars[j])) found = true
            j--
        }
        return found
    }

    /** Indeks base letter pertama mulai [from]; -1 bila tidak ada. */
    private fun nextBaseIndex(
        chars: IntArray,
        from: Int
    ): Int {
        var j = from
        while (j < chars.size) {
            val cp = chars[j]
            if (!isMarkCp(cp) && cp != 0x20 && cp != 0x09 && cp != 0x0A) return j
            j++
        }
        return -1
    }

    /** Akhir rentang base letter (base + harakat yang menyertainya). */
    private fun markEnd(
        chars: IntArray,
        base: Int
    ): Int {
        var j = base + 1
        while (j < chars.size && isMarkCp(chars[j])) j++
        return j
    }

    /** Tentukan aturan berdasar huruf berikutnya setelah nun sukun / tanween. */
    private fun ruleAfterNun(next: Int): Rule? =
        when {
            inSet(next, idghamBigSet) -> Rule.IDGHAM_BIGHUNNAH
            inSet(next, idghamSmallSet) -> Rule.IDGHAM_BILAGHUNNAH
            isBaLetter(next) -> Rule.IQLAB
            inSet(next, ikhfaSet) -> Rule.IKHFA
            inSet(next, izharSet) -> Rule.IZHAR
            else -> null
        }

    private fun analyzeNunRules(
        chars: IntArray,
        i: Int,
        ranges: MutableList<Range>
    ) {
        val triggers = isNunSukun(chars, i) || hasTanweenBefore(chars, i)
        if (!triggers) return
        val nextBase = nextBaseIndex(chars, i + 1)
        if (nextBase == -1) return
        val rule = ruleAfterNun(chars[nextBase]) ?: return
        ranges.add(Range(i, markEnd(chars, nextBase), rule))
    }

    private fun analyzeQalqalah(
        chars: IntArray,
        i: Int,
        ranges: MutableList<Range>
    ) {
        if (!inSet(chars[i], qalqalahSet)) return
        var j = i + 1
        var found = false
        while (j < chars.size && isMarkCp(chars[j])) {
            if (isSukun(chars[j])) found = true
            j++
        }
        if (found) ranges.add(Range(i, j, Rule.QALQALAH))
    }

    private fun analyzeMad(
        chars: IntArray,
        i: Int,
        ranges: MutableList<Range>
    ) {
        var j = i + 1
        var hadFathah = false
        while (j < chars.size && isMarkCp(chars[j])) {
            if (isFathah(chars[j])) hadFathah = true
            j++
        }
        if (hadFathah && j < chars.size && chars[j] == 0x0627) {
            ranges.add(Range(i, j + 1, Rule.MAD))
        }
    }

    /**
     * Analisis teks Arab (Uthmani) dan kembalikan rentang karakter [start, end)
     * untuk tiap aturan tajwid yang berlaku.
     */
    fun analyze(text: String): List<Range> {
        val ranges = mutableListOf<Range>()
        val chars = text.codePoints().toArray()
        for (i in chars.indices) {
            analyzeNunRules(chars, i, ranges)
            analyzeQalqalah(chars, i, ranges)
            analyzeMad(chars, i, ranges)
        }
        return ranges
    }
}
