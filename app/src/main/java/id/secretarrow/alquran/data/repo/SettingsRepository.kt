package id.secretarrow.alquran.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import id.secretarrow.alquran.data.model.AdzanToggles
import id.secretarrow.alquran.data.model.RasmStyle
import id.secretarrow.alquran.data.model.RepeatMode
import id.secretarrow.alquran.data.model.ThemeMode
import id.secretarrow.alquran.data.model.Translator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Preferensi pengguna, tersimpan di DataStore. */
data class AppSettings(
    val rasm: RasmStyle = RasmStyle.UTHMANI,
    val arabicFontSize: Int = 20,
    val showArabicAyahNumber: Boolean = true,
    val latinEnabled: Boolean = true,
    val latinFontSize: Int = 16,
    val translationEnabled: Boolean = true,
    val translator: Translator = Translator.KEMENAG,
    val darkMode: ThemeMode = ThemeMode.SYSTEM,
    val reciterId: Int = 1,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val adzanToggles: AdzanToggles = AdzanToggles(),
    val adzanEnabled: Boolean = true,
    val hijriOffset: Int = 0,
    val useGps: Boolean = true,
    val cityId: String = "jakarta",
    val keepScreenOn: Boolean = false
)

class SettingsRepository(
    private val context: Context
) {
    private object Keys {
        val RASM = stringPreferencesKey("rasm")
        val ARABIC_FONT_SIZE = intPreferencesKey("arabic_font_size")
        val SHOW_ARABIC_AYAH_NUMBER = booleanPreferencesKey("show_arabic_ayah_number")
        val LATIN_ENABLED = booleanPreferencesKey("latin_enabled")
        val LATIN_FONT_SIZE = intPreferencesKey("latin_font_size")
        val TRANSLATION_ENABLED = booleanPreferencesKey("translation_enabled")
        val TRANSLATOR = stringPreferencesKey("translator")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val RECITER_ID = intPreferencesKey("reciter_id")
        val REPEAT_MODE = stringPreferencesKey("repeat_mode")
        val ADZAN_ENABLED = booleanPreferencesKey("adzan_enabled")
        val ADZAN_IMSAK = booleanPreferencesKey("adzan_imsak")
        val ADZAN_SUBUH = booleanPreferencesKey("adzan_subuh")
        val ADZAN_TERBIT = booleanPreferencesKey("adzan_terbit")
        val ADZAN_DZUHUR = booleanPreferencesKey("adzan_dzuhur")
        val ADZAN_ASHAR = booleanPreferencesKey("adzan_ashar")
        val ADZAN_MAGHRIB = booleanPreferencesKey("adzan_maghrib")
        val ADZAN_ISYA = booleanPreferencesKey("adzan_isya")
        val HIJRI_OFFSET = intPreferencesKey("hijri_offset")
        val USE_GPS = booleanPreferencesKey("use_gps")
        val CITY_ID = stringPreferencesKey("city_id")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    }

    val settings: Flow<AppSettings> =
        context.dataStore.data.map { p ->
            AppSettings(
                rasm = p[Keys.RASM]?.let { runCatching { RasmStyle.valueOf(it) }.getOrNull() } ?: RasmStyle.UTHMANI,
                arabicFontSize = p[Keys.ARABIC_FONT_SIZE] ?: 20,
                showArabicAyahNumber = p[Keys.SHOW_ARABIC_AYAH_NUMBER] ?: true,
                latinEnabled = p[Keys.LATIN_ENABLED] ?: true,
                latinFontSize = p[Keys.LATIN_FONT_SIZE] ?: 16,
                translationEnabled = p[Keys.TRANSLATION_ENABLED] ?: true,
                translator = p[Keys.TRANSLATOR]?.let { runCatching { Translator.valueOf(it) }.getOrNull() } ?: Translator.KEMENAG,
                darkMode = p[Keys.DARK_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
                reciterId = p[Keys.RECITER_ID] ?: 1,
                repeatMode = p[Keys.REPEAT_MODE]?.let { runCatching { RepeatMode.valueOf(it) }.getOrNull() } ?: RepeatMode.OFF,
                adzanEnabled = p[Keys.ADZAN_ENABLED] ?: true,
                adzanToggles =
                    AdzanToggles(
                        imsak = p[Keys.ADZAN_IMSAK] ?: false,
                        subuh = p[Keys.ADZAN_SUBUH] ?: true,
                        terbit = p[Keys.ADZAN_TERBIT] ?: false,
                        dzuhur = p[Keys.ADZAN_DZUHUR] ?: true,
                        ashar = p[Keys.ADZAN_ASHAR] ?: true,
                        maghrib = p[Keys.ADZAN_MAGHRIB] ?: true,
                        isya = p[Keys.ADZAN_ISYA] ?: true
                    ),
                hijriOffset = p[Keys.HIJRI_OFFSET] ?: 0,
                useGps = p[Keys.USE_GPS] ?: true,
                cityId = p[Keys.CITY_ID] ?: "jakarta",
                keepScreenOn = p[Keys.KEEP_SCREEN_ON] ?: false
            )
        }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setRasm(v: RasmStyle) = context.dataStore.edit { it[Keys.RASM] = v.name }

    suspend fun setArabicFontSize(v: Int) = context.dataStore.edit { it[Keys.ARABIC_FONT_SIZE] = v.coerceIn(14, 42) }

    suspend fun setShowArabicAyahNumber(v: Boolean) = context.dataStore.edit { it[Keys.SHOW_ARABIC_AYAH_NUMBER] = v }

    suspend fun setLatinEnabled(v: Boolean) = context.dataStore.edit { it[Keys.LATIN_ENABLED] = v }

    suspend fun setLatinFontSize(v: Int) = context.dataStore.edit { it[Keys.LATIN_FONT_SIZE] = v.coerceIn(12, 30) }

    suspend fun setTranslationEnabled(v: Boolean) = context.dataStore.edit { it[Keys.TRANSLATION_ENABLED] = v }

    suspend fun setTranslator(v: Translator) = context.dataStore.edit { it[Keys.TRANSLATOR] = v.name }

    suspend fun setDarkMode(v: ThemeMode) = context.dataStore.edit { it[Keys.DARK_MODE] = v.name }

    suspend fun setReciterId(v: Int) = context.dataStore.edit { it[Keys.RECITER_ID] = v }

    suspend fun setRepeatMode(v: RepeatMode) = context.dataStore.edit { it[Keys.REPEAT_MODE] = v.name }

    suspend fun setAdzanEnabled(v: Boolean) = context.dataStore.edit { it[Keys.ADZAN_ENABLED] = v }

    suspend fun setAdzanToggle(
        name: String,
        v: Boolean
    ) = context.dataStore.edit {
        when (name) {
            "IMSAK" -> it[Keys.ADZAN_IMSAK] = v
            "SUBUH" -> it[Keys.ADZAN_SUBUH] = v
            "TERBIT" -> it[Keys.ADZAN_TERBIT] = v
            "DZUHUR" -> it[Keys.ADZAN_DZUHUR] = v
            "ASHAR" -> it[Keys.ADZAN_ASHAR] = v
            "MAGHRIB" -> it[Keys.ADZAN_MAGHRIB] = v
            "ISYA" -> it[Keys.ADZAN_ISYA] = v
        }
    }

    suspend fun setHijriOffset(v: Int) = context.dataStore.edit { it[Keys.HIJRI_OFFSET] = v.coerceIn(-2, 2) }

    suspend fun setUseGps(v: Boolean) = context.dataStore.edit { it[Keys.USE_GPS] = v }

    suspend fun setCityId(v: String) = context.dataStore.edit { it[Keys.CITY_ID] = v }

    suspend fun setKeepScreenOn(v: Boolean) = context.dataStore.edit { it[Keys.KEEP_SCREEN_ON] = v }
}
