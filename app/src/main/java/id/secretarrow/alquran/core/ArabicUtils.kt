package id.secretarrow.alquran.core

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Konversi & util teks Arab. */
object ArabicUtils {
    private val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    /** Ubah angka latin ke angka Arab-Indic, mis. 255 -> ٢٥٥ */
    fun toArabicNumber(value: Int): String =
        value
            .toString()
            .map { c ->
                if (c.isDigit()) arabicDigits[c - '0'] else c
            }.joinToString("")
}

/** Perhitungan arah & jarak kiblat (great-circle / haversine). */
object QiblaUtils {
    const val KAABA_LAT = 21.422487
    const val KAABA_LNG = 39.826206

    /** Sudut kiblat dari utara sejati (derajat), 0..360. */
    fun qiblaDirection(
        latitude: Double,
        longitude: Double
    ): Double {
        val phiK = Math.toRadians(KAABA_LAT)
        val lambdaK = Math.toRadians(KAABA_LNG)
        val phi = Math.toRadians(latitude)
        val lambda = Math.toRadians(longitude)
        val deltaLambda = lambdaK - lambda
        val y = sin(deltaLambda)
        val x = cos(phi) * Math.tan(phiK) - sin(phi) * cos(deltaLambda)
        val bearing = Math.toDegrees(atan2(y, x))
        return (bearing + 360.0) % 360.0
    }

    /** Jarak ke Ka'bah dalam kilometer. */
    fun distanceToKaabaKm(
        latitude: Double,
        longitude: Double
    ): Double {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(KAABA_LAT - latitude)
        val dLng = Math.toRadians(KAABA_LNG - longitude)
        val a =
            sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(latitude)) * cos(Math.toRadians(KAABA_LAT)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}
