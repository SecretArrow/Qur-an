package id.secretarrow.alquran.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import id.secretarrow.alquran.MainActivity
import id.secretarrow.alquran.R
import id.secretarrow.alquran.core.HijriHelper
import id.secretarrow.alquran.prayer.AdzanScheduler
import id.secretarrow.alquran.prayer.PrayerCalculator
import java.time.LocalDate
import java.time.ZoneId

/**
 * Widget jadwal sholat: menampilkan jadwal hari ini (gratis, tanpa premium).
 * Diperbarui tiap 30 menit, saat boot, dan saat aplikasi dibuka.
 */
class PrayerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val views = buildViews(context)
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    companion object {
        fun buildViews(context: Context): RemoteViews {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val lat = AdzanScheduler.locationLat(context)
            val lng = AdzanScheduler.locationLng(context)
            val times = PrayerCalculator.calculate(lat, lng, today, zone)
            val now = System.currentTimeMillis()

            val entries =
                listOf(
                    "Imsak" to times.imsak,
                    "Subuh" to times.subuh,
                    "Terbit" to times.terbit,
                    "Dzuhur" to times.dzuhur,
                    "Ashar" to times.ashar,
                    "Maghrib" to times.maghrib,
                    "Isya" to times.isya
                )
            val next = entries.firstOrNull { it.second > now } ?: entries.first()

            val sb = StringBuilder()
            entries.forEach { (name, millis) ->
                sb.append(name).append(" ").append(PrayerCalculator.formatTime(millis, zone))
                if (name != "Isya") sb.append("   ")
            }

            val intent = Intent(context, MainActivity::class.java)
            val pending =
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

            val views =
                RemoteViews(context.packageName, R.layout.widget_prayer_times).apply {
                    setTextViewText(R.id.widget_date, HijriHelper.format(today))
                    setTextViewText(R.id.widget_next, "${next.first} ${PrayerCalculator.formatTime(next.second, zone)}")
                    setTextViewText(R.id.widget_times, sb.toString())
                    setOnClickPendingIntent(R.id.widget_root, pending)
                }
            return views
        }

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PrayerWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                manager.updateAppWidget(ids, buildViews(context))
            }
        }
    }
}
