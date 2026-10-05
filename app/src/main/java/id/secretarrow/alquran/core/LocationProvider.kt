package id.secretarrow.alquran.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Penyedia lokasi ringan: last-known-location dari GPS/Network/Passive.
 * Tidak memakai Play Services agar aplikasi tetap ringan & offline-friendly.
 */
object LocationProvider {
    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun lastKnown(context: Context): Location? {
        if (!hasPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers =
            listOfNotNull(
                runCatching { LocationManager.NETWORK_PROVIDER }.getOrNull(),
                runCatching { LocationManager.GPS_PROVIDER }.getOrNull(),
                runCatching { LocationManager.PASSIVE_PROVIDER }.getOrNull()
            )
        var best: Location? = null
        for (p in providers) {
            try {
                val loc = lm.getLastKnownLocation(p) ?: continue
                if (best == null || loc.time > best.time) best = loc
            } catch (_: SecurityException) {
                // izin dicabut saat runtime — abaikan provider ini
            }
        }
        return best
    }

    /** Deskripsi lokasi: kota, provinsi - negara; fallback koordinat saat offline. */
    fun describe(
        context: Context,
        lat: Double,
        lng: Double
    ): String {
        val geocoder = Geocoder(context, Locale("id"))
        return try {
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            val a = addresses?.firstOrNull()
            if (a != null) {
                val city = a.locality ?: a.subAdminArea ?: a.adminArea ?: ""
                val region = a.adminArea ?: ""
                val country = a.countryName ?: ""
                listOf(city, region).filter { it.isNotBlank() }.distinct().joinToString(", ") +
                    (if (country.isNotBlank()) " - $country" else "")
            } else {
                String.format(Locale.US, "%.4f, %.4f", lat, lng)
            }
        } catch (e: IOException) {
            String.format(Locale.US, "%.4f, %.4f", lat, lng)
        } catch (e: IllegalArgumentException) {
            String.format(Locale.US, "%.4f, %.4f", lat, lng)
        }
    }

    /** Jarak km antara dua titik. */
    fun distanceKm(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a =
            sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
