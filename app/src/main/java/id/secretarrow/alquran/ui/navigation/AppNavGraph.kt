package id.secretarrow.alquran.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.secretarrow.alquran.data.model.ThemeMode
import id.secretarrow.alquran.di.AppContainer
import id.secretarrow.alquran.ui.menu.MenuScreen
import id.secretarrow.alquran.ui.prayer.PrayerCalendarScreen
import id.secretarrow.alquran.ui.prayer.PrayerScreen
import id.secretarrow.alquran.ui.prayer.PrayerViewModel
import id.secretarrow.alquran.ui.prayer.PrayerViewModelFactoryProvider
import id.secretarrow.alquran.ui.qibla.QiblaScreen
import id.secretarrow.alquran.ui.reader.ReaderScreen
import id.secretarrow.alquran.ui.search.SearchScreen
import id.secretarrow.alquran.ui.settings.SettingsScreen
import id.secretarrow.alquran.ui.surahlist.SurahListScreen
import id.secretarrow.alquran.ui.theme.AlQuranTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val SURAH_LIST = "surahlist/{tab}"
    const val READER = "reader/{surah}/{ayah}"
    const val SEARCH = "search"
    const val PRAYER = "prayer"
    const val PRAYER_CALENDAR = "prayer_calendar"
    const val QIBLA = "qibla"
    const val SETTINGS = "settings"

    fun surahList(tab: Int) = "surahlist/$tab"

    fun reader(
        surah: Int,
        ayah: Int
    ) = "reader/$surah/$ayah"
}

/** NavHost utama + tema mengikuti pengaturan. */
@Composable
fun AppNavGraph(
    container: AppContainer,
    navController: NavHostController = rememberNavController()
) {
    val darkMode by remember(container) {
        container.settingsRepository.settings.map { it.darkMode }
    }.collectAsState(initial = ThemeMode.SYSTEM)

    val lastRead by container.bookmarkRepository.lastRead().collectAsState(initial = null)
    val surahNames by remember(container) {
        container.surahNameCache
    }.collectAsState()

    AlQuranTheme(darkMode = darkMode) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME
        ) {
            composable(Routes.HOME) {
                MenuScreen(
                    lastRead = lastRead,
                    surahName = { n -> surahNames[n] ?: "Surah $n" },
                    onBacaQuran = { navController.navigate(Routes.surahList(0)) },
                    onTerakhirBaca = {
                        val lr = lastRead
                        if (lr != null) {
                            navController.navigate(Routes.reader(lr.surah, lr.ayah))
                        }
                    },
                    onPencarian = { navController.navigate(Routes.SEARCH) },
                    onJadwalSholat = { navController.navigate(Routes.PRAYER) },
                    onPengaturan = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.SURAH_LIST) { entry ->
                val tab = entry.arguments?.getString("tab")?.toIntOrNull() ?: 0
                SurahListScreen(
                    container = container,
                    initialTab = tab,
                    darkMode = darkMode,
                    onToggleTheme = {
                        val next = if (darkMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
                        container.appScope.launch { container.settingsRepository.setDarkMode(next) }
                    },
                    onBack = { navController.popBackStack() },
                    onOpenSurah = { surah, ayah -> navController.navigate(Routes.reader(surah, ayah)) },
                    onPlaySurah = { reciterId, surah, ayahCount ->
                        val reciter =
                            byId(reciterId)
                        container.audioPlayerManager.ensureController {
                            container.audioPlayerManager.playSurah(reciter, surah, ayahCount, 1)
                        }
                        navController.navigate(Routes.reader(surah, 1))
                    }
                )
            }
            composable(Routes.READER) { entry ->
                val surah = entry.arguments?.getString("surah")?.toIntOrNull() ?: 1
                val ayah = entry.arguments?.getString("ayah")?.toIntOrNull() ?: 1
                val keepScreenOn by remember(container) {
                    container.settingsRepository.settings.map { it.keepScreenOn }
                }.collectAsState(initial = false)
                ReaderScreen(
                    container = container,
                    initialSurah = surah,
                    initialAyah = ayah,
                    onBack = { navController.popBackStack() },
                    onOpenSurahList = { navController.navigate(Routes.surahList(0)) },
                    keepScreenOn = keepScreenOn
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                    onOpenAyah = { surah, ayah -> navController.navigate(Routes.reader(surah, ayah)) }
                )
            }
            composable(Routes.PRAYER) {
                PrayerScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                    onOpenQibla = { navController.navigate(Routes.QIBLA) },
                    onOpenCalendar = { navController.navigate(Routes.PRAYER_CALENDAR) }
                )
            }
            composable(Routes.PRAYER_CALENDAR) {
                PrayerCalendarScreen(
                    container = container,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.QIBLA) {
                val vm: PrayerViewModel =
                    viewModel(
                        factory = PrayerViewModelFactoryProvider(container)
                    )
                val state by vm.state.collectAsState()
                QiblaScreen(
                    onBack = { navController.popBackStack() },
                    latitude = state.lat,
                    longitude = state.lng,
                    locationLabel = state.locationLabel.ifEmpty { "Jakarta, DKI Jakarta - Indonesia" }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    container = container,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
