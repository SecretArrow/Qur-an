package id.secretarrow.alquran.ui.prayer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.secretarrow.alquran.core.LocationProvider
import id.secretarrow.alquran.data.model.AdzanToggles
import id.secretarrow.alquran.data.model.PrayerTimesOfDay
import id.secretarrow.alquran.data.repo.SettingsRepository
import id.secretarrow.alquran.prayer.AdzanScheduler
import id.secretarrow.alquran.prayer.IndonesianCities
import id.secretarrow.alquran.prayer.IndonesianCity
import id.secretarrow.alquran.prayer.PrayerCalculator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PrayerUiState(
    val loading: Boolean = true,
    val date: LocalDate = LocalDate.now(),
    val hijri: String = "",
    val gregorian: String = "",
    val times: PrayerTimesOfDay? = null,
    val locationLabel: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val adzanToggles: AdzanToggles = AdzanToggles(),
    val useGps: Boolean = true,
    val permissionDenied: Boolean = false,
    val cityId: String = "jakarta",
    val nextLabel: String = ""
)

class PrayerViewModel(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(PrayerUiState())
    val state: StateFlow<PrayerUiState> = _state.asStateFlow()

    private val appContext = context.applicationContext

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            _state.value =
                _state.value.copy(
                    adzanToggles = settings.adzanToggles,
                    useGps = settings.useGps,
                    cityId = settings.cityId
                )
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val s = _state.value
            val (lat, lng, label) = resolveLocation()
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val times = PrayerCalculator.calculate(lat, lng, today, zone)
            _state.value =
                s.copy(
                    loading = false,
                    date = today,
                    hijri =
                        id.secretarrow.alquran.core.HijriHelper
                            .format(today, settingsRepository.current().hijriOffset),
                    gregorian =
                        id.secretarrow.alquran.core.HijriHelper
                            .formatGregorian(today),
                    times = times,
                    lat = lat,
                    lng = lng,
                    locationLabel = label
                )
            updateNextLabel()
            // Simpan lokasi untuk alarm & widget
            AdzanScheduler.saveLocation(appContext, lat, lng)
        }
    }

    private fun resolveLocation(): Triple<Double, Double, String> {
        val settings = settingsRepository.current()
        return if (settings.useGps) {
            val location = LocationProvider.lastKnown(appContext)
            if (location != null) {
                val label = LocationProvider.describe(appContext, location.latitude, location.longitude)
                Triple(location.latitude, location.longitude, label)
            } else {
                val city = IndonesianCities.byId(settings.cityId)
                Triple(city.lat, city.lng, cityLabel(city))
            }
        } else {
            val city = IndonesianCities.byId(settings.cityId)
            Triple(city.lat, city.lng, cityLabel(city))
        }
    }

    private fun cityLabel(city: IndonesianCity): String = "${city.name}, ${city.region} - Indonesia"

    fun updateNextLabel() {
        val times = _state.value.times ?: return
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        val nowMillis = now.atZone(zone).toInstant().toEpochMilli()
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
        val next = entries.firstOrNull { it.second > nowMillis } ?: entries.first()
        val diffMinutes = ((next.second - nowMillis) / 60_000L).coerceAtLeast(0)
        _state.value = _state.value.copy(nextLabel = "${next.first} ± $diffMinutes menit lagi")
    }

    fun toggleAdzan(
        name: String,
        enabled: Boolean
    ) {
        viewModelScope.launch {
            settingsRepository.setAdzanToggle(name, enabled)
            val t =
                when (name) {
                    "IMSAK" -> _state.value.adzanToggles.copy(imsak = enabled)
                    "SUBUH" -> _state.value.adzanToggles.copy(subuh = enabled)
                    "TERBIT" -> _state.value.adzanToggles.copy(terbit = enabled)
                    "DZUHUR" -> _state.value.adzanToggles.copy(dzuhur = enabled)
                    "ASHAR" -> _state.value.adzanToggles.copy(ashar = enabled)
                    "MAGHRIB" -> _state.value.adzanToggles.copy(maghrib = enabled)
                    "ISYA" -> _state.value.adzanToggles.copy(isya = enabled)
                    else -> _state.value.adzanToggles
                }
            _state.value = _state.value.copy(adzanToggles = t)
            persistAndSchedule(t)
        }
    }

    private fun persistAndSchedule(toggles: AdzanToggles) {
        val active =
            buildSet {
                if (toggles.imsak) add(id.secretarrow.alquran.data.model.PrayerName.IMSAK)
                if (toggles.subuh) add(id.secretarrow.alquran.data.model.PrayerName.SUBUH)
                if (toggles.terbit) add(id.secretarrow.alquran.data.model.PrayerName.TERBIT)
                if (toggles.dzuhur) add(id.secretarrow.alquran.data.model.PrayerName.DZUHUR)
                if (toggles.ashar) add(id.secretarrow.alquran.data.model.PrayerName.ASHAR)
                if (toggles.maghrib) add(id.secretarrow.alquran.data.model.PrayerName.MAGHRIB)
                if (toggles.isya) add(id.secretarrow.alquran.data.model.PrayerName.ISYA)
            }
        appContext
            .getSharedPreferences("adzan_toggles", Context.MODE_PRIVATE)
            .edit()
            .putStringSet("active", active.map { it.name }.toSet())
            .apply()
        AdzanScheduler.scheduleAll(appContext, active)
    }

    fun setUseGps(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setUseGps(enabled)
            _state.value = _state.value.copy(useGps = enabled)
            refresh()
        }
    }

    fun setCity(cityId: String) {
        viewModelScope.launch {
            settingsRepository.setCityId(cityId)
            _state.value = _state.value.copy(cityId = cityId)
            refresh()
        }
    }

    fun markPermissionDenied() {
        _state.value = _state.value.copy(permissionDenied = true)
    }

    class Factory(
        private val context: Context,
        private val container: id.secretarrow.alquran.di.AppContainer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PrayerViewModel(context, container.settingsRepository) as T
    }
}
