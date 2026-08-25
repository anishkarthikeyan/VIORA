package com.viora.app.presentation.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viora.app.domain.model.VioraContext
import com.viora.app.domain.threat.RiskLevel
import com.viora.app.domain.threat.ThreatAssessment
import com.viora.app.presentation.components.RiskDangerousColor
import com.viora.app.presentation.components.RiskSafeColor
import com.viora.app.presentation.components.RiskSuspiciousColor
import com.viora.app.presentation.components.RiskVerifyColor
import com.viora.app.presentation.components.VioraDarkBackground
import com.viora.app.presentation.components.VioraDarkSurface
import com.viora.app.presentation.components.VioraPrimaryCyan

/**
 * Detailed explanation of a completed analysis. Purely presentational: the overlay
 * remains the primary warning; this screen only explains WHAT / WHY / EVIDENCE / ACTION.
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
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DETAILED ASSESSMENT",
                color = VioraPrimaryCyan,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (assessment == null) {
                EmptyState(modifier = Modifier.weight(1f))
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    RiskHeader(assessment)
                    WhatVioraFound(assessment)
                    WhyItMatters(assessment)
                    Evidence(context)
                    RecommendedAction(assessment)
                }
            }

            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VioraPrimaryCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Back to Scanner", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RiskHeader(assessment: ThreatAssessment) {
    val riskColor = assessment.riskLevel.color()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(riskColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Risk: ${assessment.riskLevel.name}",
            color = riskColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${assessment.score}/100",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        color = VioraDarkSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun WhatVioraFound(assessment: ThreatAssessment) {
    SectionCard("WHAT VIORA FOUND") {
        val lines = assessment.signals.map { "• ${signalLabel(it.id)}" }
            .ifEmpty { listOf("• ${assessment.riskLevel.name}: no specific signals") }
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
    SectionCard("WHY IT MATTERS") {
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
    SectionCard("EVIDENCE") {
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
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
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
    SectionCard("RECOMMENDED ACTION") {
        Text(
            text = assessment.recommendedAction,
            color = assessment.riskLevel.color(),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
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
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Scan a QR code or payment link first — the full breakdown of that check will appear here.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
    }
}

/** Human-readable names for deterministic signal IDs. */
private fun signalLabel(id: String): String = when (id) {
    "AMOUNT_MISMATCH" -> "Amount mismatch"
    "MERCHANT_MISMATCH" -> "Merchant mismatch"
    "UNKNOWN_RECIPIENT" -> "Unverified recipient"
    else -> id.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}

private fun RiskLevel.color(): Color = when (this) {
    RiskLevel.SAFE -> RiskSafeColor
    RiskLevel.VERIFY -> RiskVerifyColor
    RiskLevel.SUSPICIOUS -> RiskSuspiciousColor
    RiskLevel.DANGEROUS -> RiskDangerousColor
}
