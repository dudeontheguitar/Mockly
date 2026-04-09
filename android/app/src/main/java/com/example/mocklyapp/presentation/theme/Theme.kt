package com.example.mocklyapp.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = Color.White,
    secondary = Secondary,
    onSecondary = Color.White,
    background = BackgroundDark,
    onBackground = OnDarkSubtle,
    surface = SurfaceDark,
    onSurface = OnDarkSubtle,
    error = Error,
    onError = Color.White
)

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E9FF),
    onPrimaryContainer = PrimaryContainer,
    secondary = Secondary,
    onSecondary = Color.White,
    background = Color(0xFFF8F8F8),
    onBackground = PrimaryContainer,
    surface = Color.White,
    onSurface = PrimaryContainer,
    error = Error,
    onError = Color.White
)

@Composable
fun MocklyTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
