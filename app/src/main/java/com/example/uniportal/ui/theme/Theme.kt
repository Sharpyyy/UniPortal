package com.example.uniportal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.uniportal.domain.model.ThemeMode

private val PortalColorScheme = darkColorScheme(
    primary = PortalGreen,
    onPrimary = OnPortalGreen,
    primaryContainer = PortalGreenContainer,
    onPrimaryContainer = OnPortalGreenContainer,
    inversePrimary = PortalGreenContainer,
    secondary = PortalCyan,
    onSecondary = OnPortalCyan,
    secondaryContainer = PortalCyanContainer,
    onSecondaryContainer = OnPortalCyanContainer,
    tertiary = PortalViolet,
    onTertiary = OnPortalViolet,
    tertiaryContainer = PortalVioletContainer,
    onTertiaryContainer = OnPortalVioletContainer,
    background = PortalBackground,
    onBackground = PortalOnBackground,
    surface = PortalSurface,
    onSurface = PortalOnBackground,
    surfaceVariant = PortalSurfaceContainer,
    onSurfaceVariant = PortalOnSurfaceVariant,
    surfaceTint = PortalGreen,
    inverseSurface = PortalOnBackground,
    inverseOnSurface = PortalBackground,
    error = PortalError,
    onError = OnPortalError,
    errorContainer = PortalErrorContainer,
    onErrorContainer = OnPortalErrorContainer,
    outline = PortalOutline,
    outlineVariant = PortalOutlineVariant,
    scrim = Color.Black,
    surfaceBright = PortalSurfaceContainerHighest,
    surfaceContainer = PortalSurfaceContainer,
    surfaceContainerHigh = PortalSurfaceContainerHigh,
    surfaceContainerHighest = PortalSurfaceContainerHighest,
    surfaceContainerLow = PortalSurfaceContainerLow,
    surfaceContainerLowest = PortalSurfaceContainerLowest,
    surfaceDim = PortalBackground,
)

private val LightPortalColorScheme = lightColorScheme(
    primary = LightGreen,
    onPrimary = OnLightGreen,
    primaryContainer = LightGreenContainer,
    onPrimaryContainer = OnLightGreenContainer,
    inversePrimary = PortalGreen,
    secondary = LightCyan,
    onSecondary = OnLightCyan,
    secondaryContainer = LightCyanContainer,
    onSecondaryContainer = OnLightCyanContainer,
    tertiary = LightViolet,
    onTertiary = OnLightViolet,
    tertiaryContainer = LightVioletContainer,
    onTertiaryContainer = OnLightVioletContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceContainerHigh,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = LightGreen,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightSurfaceContainer,
    error = LightError,
    onError = OnLightError,
    errorContainer = LightErrorContainer,
    onErrorContainer = OnLightErrorContainer,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    scrim = Color.Black,
    surfaceBright = LightSurface,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceDim = LightSurfaceDim,
)

@Immutable
data class PortalColors(

    val favourite: Color,

    val warning: Color,

    val info: Color,
    val isDark: Boolean,
)

private val DarkPortalColors = PortalColors(
    favourite = FavouriteAmber,
    warning = FavouriteAmber,
    info = PortalInfoBlue,
    isDark = true,
)

private val LightPortalColors = PortalColors(
    favourite = LightFavouriteAmber,
    warning = LightWarningAmber,
    info = LightInfoBlue,
    isDark = false,
)

private val LocalPortalColors = staticCompositionLocalOf { DarkPortalColors }

object PortalTheme {
    val colors: PortalColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPortalColors.current
}

fun ThemeMode.isDarkTheme(systemInDarkTheme: Boolean): Boolean = when (this) {
    ThemeMode.PORTAL -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> systemInDarkTheme
}

@Composable
fun UniPortalTheme(
    themeMode: ThemeMode = ThemeMode.PORTAL,
    content: @Composable () -> Unit,
) {
    val darkTheme = themeMode.isDarkTheme(isSystemInDarkTheme())
    val colorScheme: ColorScheme = if (darkTheme) PortalColorScheme else LightPortalColorScheme
    CompositionLocalProvider(LocalPortalColors provides if (darkTheme) DarkPortalColors else LightPortalColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = UniPortalTypography,
            shapes = UniPortalShapes,
            content = content,
        )
    }
}
