package com.viora.app.presentation.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Premium Obsidian Security Color Palette
val VioraDarkBackground = Color(0xFF0B0E14)
val VioraDarkSurface = Color(0xFF151A23)
val VioraDarkSurfaceVariant = Color(0xFF1E2532)
val VioraPrimaryCyan = Color(0xFF00E5FF)
val VioraOnPrimaryDark = Color(0xFF00363A)

// Risk Colors
val RiskSafeColor = Color(0xFF00E676)
val RiskVerifyColor = Color(0xFFFFD600)
val RiskSuspiciousColor = Color(0xFFFF9100)
val RiskDangerousColor = Color(0xFFFF1744)

private val VioraDarkColorScheme = darkColorScheme(
    primary = VioraPrimaryCyan,
    onPrimary = VioraOnPrimaryDark,
    background = VioraDarkBackground,
    surface = VioraDarkSurface,
    surfaceVariant = VioraDarkSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFF90A4AE)
)

@Composable
fun VioraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VioraDarkColorScheme,
        content = content
    )
}
