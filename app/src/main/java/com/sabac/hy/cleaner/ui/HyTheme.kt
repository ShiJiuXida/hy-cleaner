package com.sabac.hy.cleaner.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF2F6FED),
    secondaryContainer = Color(0xFFD9E4FF),
    onSecondaryContainer = Color(0xFF10305E),
    surface = Color(0xFFF7F8FA),
    surfaceContainer = Color(0xFFFFFFFF),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF9DBBFF),
    secondaryContainer = Color(0xFF2A3F66),
    onSecondaryContainer = Color(0xFFD9E4FF),
    surface = Color(0xFF111318),
    surfaceContainer = Color(0xFF1A1D24),
)

@Composable
fun HyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content,
    )
}
