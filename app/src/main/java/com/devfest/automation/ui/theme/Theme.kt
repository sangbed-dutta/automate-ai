package com.devfest.automation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    background = BackgroundLight,
    surface = Color.White,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF1F5F9), // Slate 100
    onSurfaceVariant = TextSecondaryLight
)

private val DarkColors = darkColorScheme(
    primary = ElectricBlue, // Keep electric blue for brand, maybe slightly lighter?
    onPrimary = Color.White,
    background = BackgroundDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF1E293B), // Slate 800
    onSurfaceVariant = TextSecondaryDark
)

@Composable
fun AgentTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDarkTheme = true // Force dark theme for "Agent" look
    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
