package id.secretarrow.alquran.core

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Konversi & format kalender Hijriah (Umm al-Qura) dengan nama bulan Indonesia. */
object HijriHelper {
    private val monthNames =
        listOf(
            "Muharam",
            "Safar",
            "Rabiulawal",
            "Rabiulakhir",
            "Jumadilawal",
            "Jumadilakhir",
            "Rajab",
            "Sya'ban",
            "Ramadhan",
            "Syawal",
            "Zulkaidah",
            "Zulhijah"
        )

    /**
     * Format tanggal masehi ke hijriah Indonesia, mis. "22 Sya'ban 1443 H".
     * [offsetDays] untuk koreksi sesuai konvensi lokal (-2..+2).
     */
    fun format(
        date: LocalDate,
        offsetDays: Int = 0
    ): String {
        val target = date.plusDays(offsetDays.toLong())
        val hijri = HijrahDate.from(target)
        val day = hijri.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
        val month = hijri.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
        val year = hijri.get(java.time.temporal.ChronoField.YEAR)
        val monthName = monthNames.getOrElse(month - 1) { "" }
        return "$day $monthName $year H"
    }

    fun toHijriDate(
        date: LocalDate,
        offsetDays: Int = 0
    ): HijrahDate = HijrahDate.from(date.plusDays(offsetDays.toLong()))

    /** Nama hari Indonesia. */
    fun dayName(date: LocalDate): String {
        val names = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
        return names[date.dayOfWeek.value - 1]
    }

    /** Format tanggal masehi Indonesia, mis. "26 Maret 2022". */
    fun formatGregorian(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("id"))
        return date.format(formatter)
    }

    /** Format pendek "10 Apr 17". */
    fun formatGregorianShort(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("d MMM yy", Locale("id"))
        return date.format(formatter)
    }
}
