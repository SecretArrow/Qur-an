package id.secretarrow.alquran.prayer

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import id.secretarrow.alquran.R
import id.secretarrow.alquran.data.model.PrayerName
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Alarm adzan: menjadwalkan notifikasi pada setiap waktu sholat yang aktif.
 * Fallback inexact bila izin alarm presisi tidak diberikan.
 */
object AdzanScheduler {
    const val CHANNEL_ID = "adzan_channel"
    private const val REQUEST_OFFSET = 81000 // hindari bentrok request code lain

    fun scheduleAll(
        context: Context,
        toggles: Set<PrayerName>
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        val today = LocalDate.now(zone)
        val times = PrayerCalculator.calculate(locationLat(context), locationLng(context), today)
        val map =
            mapOf(
                PrayerName.IMSAK to times.imsak,
                PrayerName.SUBUH to times.subuh,
                PrayerName.TERBIT to times.terbit,
                PrayerName.DZUHUR to times.dzuhur,
                PrayerName.ASHAR to times.ashar,
                PrayerName.MAGHRIB to times.maghrib,
                PrayerName.ISYA to times.isya
            )
        val canExact =
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
        map.forEach { (name, millis) ->
            if (name in toggles) {
                // Jadwalkan untuk hari ini bila belum lewat, dan besok.
                var trigger = millis
                if (trigger <= System.currentTimeMillis()) {
                    trigger = millis + 24 * 60 * 60_000L
                }
                val intent =
                    Intent(context, AdzanReceiver::class.java)
                        .putExtra(AdzanReceiver.EXTRA_PRAYER, name.name)
                val pending =
                    android.app.PendingIntent.getBroadcast(
                        context,
                        REQUEST_OFFSET + name.ordinal,
                        intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                if (canExact) {
                    alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, trigger, pending)
                } else {
                    alarmManager.setWindow(android.app.AlarmManager.RTC_WAKEUP, trigger, 60_000L, pending)
                }
            }
        }
    }

    fun prayerDisplayName(name: PrayerName): String =
        when (name) {
            PrayerName.IMSAK -> "Imsak"
            PrayerName.SUBUH -> "Subuh"
            PrayerName.TERBIT -> "Terbit"
            PrayerName.DZUHUR -> "Dzuhur"
            PrayerName.ASHAR -> "Ashar"
            PrayerName.MAGHRIB -> "Maghrib"
            PrayerName.ISYA -> "Isya"
        }

    // Simpan lokasi sederhana (SharedPreferences) agar receiver tidak butuh repo berat.
    private const val PREFS = "prayer_location"
    private const val KEY_LAT = "lat"
    private const val KEY_LNG = "lng"

    fun saveLocation(
        context: Context,
        lat: Double,
        lng: Double
    ) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAT, java.lang.Double.doubleToRawLongBits(lat))
            .putLong(KEY_LNG, java.lang.Double.doubleToRawLongBits(lng))
            .apply()
    }

    fun locationLat(context: Context): Double =
        java.lang.Double
            .longBitsToDouble(
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_LAT, 0L)
            ).takeIf { it != 0.0 } ?: IndonesianCities.byId("jakarta").lat

    fun locationLng(context: Context): Double =
        java.lang.Double
            .longBitsToDouble(
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_LNG, 0L)
            ).takeIf { it != 0.0 } ?: IndonesianCities.byId("jakarta").lng
}

class AdzanReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val prayerName = intent.getStringExtra(EXTRA_PRAYER) ?: return
        val name = runCatching { PrayerName.valueOf(prayerName) }.getOrNull() ?: return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED ||
            android.os.Build.VERSION.SDK_INT < 33
        ) {
            val title = "Waktu ${AdzanScheduler.prayerDisplayName(name)}"
            val body = "Sudah masuk waktu ${AdzanScheduler.prayerDisplayName(name)}. Mari tunaikan sholat."
            val notification =
                NotificationCompat
                    .Builder(context, AdzanScheduler.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()
            NotificationManagerCompat.from(context).notify(name.ordinal + 100, notification)
        }
        // Jadwalkan ulang rangkaian alarm untuk siklus berikutnya.
        val prefs = context.getSharedPreferences("adzan_toggles", Context.MODE_PRIVATE)
        val active = prefs.getStringSet("active", setOf("SUBUH", "DZUHUR", "ASHAR", "MAGHRIB", "ISYA")) ?: emptySet()
        AdzanScheduler.scheduleAll(context, active.mapNotNull { runCatching { PrayerName.valueOf(it) }.getOrNull() }.toSet())
    }

    companion object {
        const val EXTRA_PRAYER = "prayer"
    }
}

class BootReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("adzan_toggles", Context.MODE_PRIVATE)
            val active = prefs.getStringSet("active", emptySet()) ?: emptySet()
            AdzanScheduler.scheduleAll(
                context,
                active.mapNotNull { runCatching { PrayerName.valueOf(it) }.getOrNull() }.toSet()
            )
        }
    }
}
