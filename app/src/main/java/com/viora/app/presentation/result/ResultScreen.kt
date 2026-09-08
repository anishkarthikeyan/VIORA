package com.viora.app.presentation.result

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viora.app.domain.model.VioraContext
import com.viora.app.domain.threat.ThreatAssessment
import com.viora.app.presentation.components.SectionHeader
import com.viora.app.presentation.components.SecondaryActionButton
import com.viora.app.presentation.components.VioraCard
import com.viora.app.presentation.components.VioraDarkBackground
import com.viora.app.presentation.components.VioraPrimaryCyan
import com.viora.app.presentation.components.VioraSpacing
import com.viora.app.presentation.components.color
import com.viora.app.presentation.components.headline
import com.viora.app.presentation.components.signalLabel

/**
 * Detailed explanation of a completed analysis — the main demo payoff screen.
 * Purely presentational: renders the real [ThreatAssessment]/[VioraContext] handed
 * in by ScannerViewModel; never computes or alters risk itself.
 */
@Composable
fun ResultScreen(
    assessment: ThreatAssessment?,
    context: VioraContext?,
    onBackClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = VioraDarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(VioraSpacing.xl)
        ) {
            Spacer(modifier = Modifier.height(VioraSpacing.sm))

            Text(
                text = "ASSESSMENT",
                color = VioraPrimaryCyan,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(VioraSpacing.lg))

            if (assessment == null) {
                EmptyState(modifier = Modifier.weight(1f))
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(VioraSpacing.md)
                ) {
                    RiskHero(assessment)
                    WhatVioraFound(assessment)
                    WhyItMatters(assessment)
                    Evidence(context)
                    RecommendedAction(assessment)
                }
                Spacer(modifier = Modifier.height(VioraSpacing.lg))
            }

            SecondaryActionButton(text = "Back to Scanner", onClick = onBackClick)
        }
    }
}

/**
 * The headline block: risk level, one-line verdict, and the numeric score as an
 * animated fill — the single strongest visual moment in the app, since this is
 * the demo payoff (Phase 6 §8).
 */
@Composable
private fun RiskHero(assessment: ThreatAssessment) {
    val riskColor = assessment.riskLevel.color()
    val animatedScore = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(assessment.score) { animatedScore.floatValue = assessment.score.toFloat() }
    val progress by animateFloatAsState(
        targetValue = (animatedScore.floatValue / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "riskScoreReveal"
    )

    VioraCard(padding = PaddingValues(VioraSpacing.xl)) {
        Text(
            text = assessment.riskLevel.name,
            color = riskColor,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(VioraSpacing.xs))
        Text(
            text = assessment.riskLevel.headline(),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(VioraSpacing.lg))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${assessment.score}",
                color = riskColor,
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = " / 100",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(VioraSpacing.sm))

        // Score track: a quiet, precise visualization — not a gauge/gimmick.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                    RoundedCornerShape(3.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .background(riskColor, RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun WhatVioraFound(assessment: ThreatAssessment) {
    VioraCard {
        SectionHeader("WHAT VIORA FOUND")
        Spacer(modifier = Modifier.height(VioraSpacing.sm))
        val lines = assessment.signals.map { "•  ${signalLabel(it.id)}" }
            .ifEmpty { listOf("•  ${assessment.riskLevel.name}: no specific signals") }
        Text(
            text = lines.joinToString("\n"),
            color = Color.White,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun WhyItMatters(assessment: ThreatAssessment) {
    VioraCard {
        SectionHeader("WHY IT MATTERS")
        Spacer(modifier = Modifier.height(VioraSpacing.sm))
        val reasons = assessment.signals.take(2)
            .map { it.description }
            .ifEmpty { listOf(assessment.explanation) }
        Text(
            text = reasons.joinToString("\n\n"),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun Evidence(context: VioraContext?) {
    VioraCard {
        SectionHeader("EVIDENCE")
        Spacer(modifier = Modifier.height(VioraSpacing.sm))
        if (context == null) {
            Text(
                text = "No structured payload was captured for this check.",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp
            )
        } else {
            EvidenceLine("Visible merchant (screen)", context.visibleMerchant ?: "—")
            EvidenceLine("Payment recipient (QR)", context.upiId ?: "—")
            context.merchantName?.let { EvidenceLine("Merchant name in payload", it) }
            EvidenceLine("Displayed amount (screen)", context.displayedAmount ?: "—")
            EvidenceLine("Requested amount (payload)", context.amount ?: "—")
        }
    }
}

@Composable
private fun EvidenceLine(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = "$label:  ",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RecommendedAction(assessment: ThreatAssessment) {
    val riskColor = assessment.riskLevel.color()
    VioraCard {
        SectionHeader("RECOMMENDED ACTION")
        Spacer(modifier = Modifier.height(VioraSpacing.sm))
        Text(
            text = assessment.recommendedAction,
            color = riskColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Nothing analyzed yet",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(VioraSpacing.xs))
        Text(
            text = "Scan a QR code or payment link first — the full breakdown of that check will appear here.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = VioraSpacing.xl)
        )
    }
}
