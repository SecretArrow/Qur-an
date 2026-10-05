package id.secretarrow.alquran

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import id.secretarrow.alquran.di.AppContainer
import id.secretarrow.alquran.prayer.AdzanScheduler
import id.secretarrow.alquran.widget.PrayerWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlQuranApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannel()
        container.appScope.launch(Dispatchers.Default) {
            PrayerWidgetProvider.updateAll(this@AlQuranApp)
        }
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                AdzanScheduler.CHANNEL_ID,
                getString(R.string.channel_adzan),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_adzan_desc)
            }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
