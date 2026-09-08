package com.viora.app.presentation.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ============================================================================
// Viora design system — "Premium Obsidian Security" palette + spacing/radius
// scale. Centralized here so screens consume tokens instead of scattering
// magic colors/dimensions. Risk-state color/label mapping lives in
// RiskPresentation.kt (depends on the domain RiskLevel enum).
// ============================================================================

// ---- Core palette ----------------------------------------------------------
val VioraDarkBackground = Color(0xFF0B0E14)
val VioraDarkSurface = Color(0xFF151A23)
val VioraDarkSurfaceVariant = Color(0xFF1E2532)
val VioraPrimaryCyan = Color(0xFF00E5FF)
val VioraOnPrimaryDark = Color(0xFF00363A)
val VioraTextSecondary = Color(0xFF90A4AE)
val VioraDivider = Color(0xFF232A38)

// ---- Risk state colors (reserved for risk communication — see RiskPresentation.kt) ----
val RiskSafeColor = Color(0xFF00E676)
val RiskVerifyColor = Color(0xFFFFD600)
val RiskSuspiciousColor = Color(0xFFFF9100)
val RiskDangerousColor = Color(0xFFFF1744)

/** Spacing scale — use instead of ad hoc `.dp` literals for margins/gaps. */
object VioraSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

/** Corner-radius scale — use instead of ad hoc `RoundedCornerShape(n.dp)` literals. */
object VioraRadius {
    val sm = 10.dp
    val md = 14.dp
    val lg = 18.dp
    val xl = 22.dp
}

private val VioraDarkColorScheme = darkColorScheme(
    primary = VioraPrimaryCyan,
    onPrimary = VioraOnPrimaryDark,
    background = VioraDarkBackground,
    surface = VioraDarkSurface,
    surfaceVariant = VioraDarkSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = VioraTextSecondary
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
