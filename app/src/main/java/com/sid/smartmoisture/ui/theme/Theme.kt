package com.sid.smartmoisture.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Primary,
    secondary = Secondary,
    background = BackgroundLight,
    surface = SurfaceLight,
    error = Error
)
private val DarkColors = darkColorScheme(
    primary = Primary,
    secondary = Secondary,
    background = BackgroundDark,
    surface = SurfaceDark,
    error = Error
)

@Composable
fun SmartMoistureTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
    typography = Typography,
    content = content
)
