package id.secretarrow.alquran.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import id.secretarrow.alquran.data.model.ThemeMode

private val LightColors =
    lightColorScheme(
        primary = Teal,
        onPrimary = LightSurface,
        primaryContainer = TealContainer,
        onPrimaryContainer = GreenDarkDeep,
        secondary = Gold,
        onSecondary = Color(0xFF3E2F1B),
        secondaryContainer = OrnamentBg,
        onSecondaryContainer = Color(0xFF3E2F1B),
        background = LightBackground,
        onBackground = LightOnSurface,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = Cream,
        onSurfaceVariant = LightSubtitle,
        error = RedTajwid
    )

private val DarkColors =
    darkColorScheme(
        primary = TealSoft,
        onPrimary = Color(0xFF00312B),
        primaryContainer = Color(0xFF005048),
        onPrimaryContainer = TealContainer,
        secondary = Gold,
        onSecondary = Color(0xFF2B2110),
        secondaryContainer = Color(0xFF3E3421),
        onSecondaryContainer = GoldBright,
        background = DarkBackground,
        onBackground = DarkOnSurface,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkSubtitle,
        error = Color(0xFFEF9A9A)
    )

/** Tema aplikasi dengan dukungan terang/gelap/sistem. */
@Composable
fun AlQuranTheme(
    darkMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark =
        when (darkMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
