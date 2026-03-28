package com.skeshmiri.everyday.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Clay,
    onPrimary = Mist,
    secondary = Moss,
    onSecondary = Mist,
    background = Sand,
    onBackground = Charcoal,
    surface = Mist,
    onSurface = Charcoal,
    surfaceVariant = Stone,
    onSurfaceVariant = Charcoal,
)

private val DarkColors = darkColorScheme(
    primary = Sand,
    onPrimary = Charcoal,
    secondary = Stone,
    onSecondary = Charcoal,
    background = Charcoal,
    onBackground = Mist,
    surface = Rust,
    onSurface = Mist,
    surfaceVariant = Moss,
    onSurfaceVariant = Mist,
)

@Composable
fun EverydayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}

