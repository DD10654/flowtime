package com.flowtime.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoContainer,
    onPrimaryContainer = IndigoStrong,
    secondary = Emerald,
    onSecondary = Color.White,
    tertiary = Emerald,
    background = SlateBg,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = Muted,
    outline = SlateBorder,
    outlineVariant = SlateBorder,
    error = Danger,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoStrong,
    onPrimaryContainer = Color.White,
    secondary = Emerald,
    onSecondary = Color.White,
    tertiary = Emerald,
    background = DarkBg,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = Color(0xFFF87171),
    onError = Color.White,
)

@Composable
fun FlowtimeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = FlowtimeTypography,
        content = content,
    )
}
