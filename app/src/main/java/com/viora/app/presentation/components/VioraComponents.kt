package com.viora.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viora.app.domain.threat.RiskLevel

/**
 * Small set of reusable Compose building blocks shared across Home/Scanner/Result/
 * History — reduces the copy-pasted Card/Text/Row boilerplate each screen had
 * before Phase 6 into one consistent, testable-by-inspection vocabulary.
 */

/** Standard elevated content card — same surface, corner radius and padding everywhere. */
@Composable
fun VioraCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(VioraSpacing.lg),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = VioraDarkSurface),
        shape = RoundedCornerShape(VioraRadius.md)
    ) {
        Column(modifier = Modifier.padding(padding)) { content() }
    }
}

/** Small uppercase eyebrow label used above a card's/section's main content. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
        modifier = modifier
    )
}

/** Colored dot + risk-level text. The one place risk color/label is rendered. */
@Composable
fun RiskBadge(
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier,
    dotSize: androidx.compose.ui.unit.Dp = 10.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp
) {
    val color = riskLevel.color()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(modifier = Modifier.size(dotSize).background(color, CircleShape))
        Spacer(modifier = Modifier.size(VioraSpacing.sm))
        Text(
            text = riskLevel.shortLabel(),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            letterSpacing = 0.5.sp
        )
    }
}

/** Full-width primary call-to-action button — the app's one accent-filled button style. */
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(VioraRadius.sm),
        colors = ButtonDefaults.buttonColors(containerColor = VioraPrimaryCyan, contentColor = Color.Black)
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

/** Full-width secondary/outline button — the app's one outline button style. */
@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(VioraRadius.sm)
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
    }
}

/**
 * Intentional empty state — used wherever a list can legitimately be empty
 * (no history yet, no recent checks) instead of a blank screen.
 */
@Composable
fun VioraEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    VioraCard(modifier = modifier) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(VioraSpacing.xs))
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

/** Thin translucent debug/dev panel surface — kept visually distinct from real content cards. */
@Composable
fun VioraDebugPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        color = VioraDarkSurface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(VioraRadius.sm),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(VioraSpacing.md)) { content() }
    }
}
