package id.secretarrow.alquran.prayer

import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.Qibla
import com.batoulapps.adhan.data.DateComponents
import id.secretarrow.alquran.data.model.PrayerTimesOfDay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

/**
 * Wrapper perhitungan waktu sholat dengan parameter konvensi Kemenag RI:
 * Subuh 20°, Isya 18°, madhab Syafi'i. Imsak = Subuh - 10 menit.
 */
object PrayerCalculator {
    fun kemenagParameters(): CalculationParameters {
        val params = CalculationParameters(20.0, 18.0)
        params.madhab = com.batoulapps.adhan.Madhab.SHAFI
        return params
    }

    fun calculate(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        zone: ZoneId = ZoneId.systemDefault()
    ): PrayerTimesOfDay {
        val coords = Coordinates(latitude, longitude)
        val params = kemenagParameters()
        val dateComponents = DateComponents(date.year, date.monthValue, date.dayOfMonth)
        val times = PrayerTimes(coords, dateComponents, params)
        val toMillis = { d: Date? -> d?.time ?: 0L }
        val subuh = times.fajr?.time ?: 0L
        val imsak = subuh - 10 * 60_000L
        return PrayerTimesOfDay(
            imsak = imsak,
            subuh = subuh,
            terbit = toMillis(times.sunrise),
            dzuhur = toMillis(times.dhuhr),
            ashar = toMillis(times.asr),
            maghrib = toMillis(times.maghrib),
            isya = toMillis(times.isha)
        )
    }

    /** Tabel jadwal 30 hari ke depan mulai [start]. */
    fun calculateRange(
        latitude: Double,
        longitude: Double,
        start: LocalDate,
        days: Int,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<Pair<LocalDate, PrayerTimesOfDay>> =
        (0 until days)
            .map { start.plusDays(it.toLong()) }
            .map { it to calculate(latitude, longitude, it, zone) }

    /** Sudut kiblat via pustaka Adhan. */
    fun qiblaDirection(
        latitude: Double,
        longitude: Double
    ): Double = Qibla(Coordinates(latitude, longitude)).direction

    /** "04:30" dari epoch millis di zona waktu [zone]. */
    fun formatTime(
        millis: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): String {
        if (millis <= 0L) return "-"
        val ldt = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), zone)
        return "%02d:%02d".format(ldt.hour, ldt.minute)
    }
}
