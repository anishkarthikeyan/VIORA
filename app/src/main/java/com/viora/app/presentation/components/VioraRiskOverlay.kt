package com.viora.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viora.app.domain.model.VioraContext
import com.viora.app.domain.threat.RiskLevel
import com.viora.app.domain.threat.ThreatAssessment

/** Surface opacity tuned so text stays readable over bright camera footage. */
private val OverlaySurface = Color(0xFF10141D).copy(alpha = 0.94f)

/**
 * The real-time risk overlay. Renders ON TOP of the live camera preview — it never
 * replaces the camera screen and never navigates away by itself.
 *
 * Driven entirely by a [ThreatAssessment] from the analysis pipeline; no hard-coded
 * UI states. Dismiss/Details behavior is delegated to the caller.
 */
@Composable
fun VioraRiskOverlay(
    assessment: ThreatAssessment?,
    visible: Boolean,
    modifier: Modifier = Modifier,
    context: VioraContext? = null,
    onDetailsClick: (() -> Unit)? = null,
    onDismissClick: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = visible && assessment != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }) + expandVertically(),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 }) + shrinkVertically(),
        modifier = modifier
    ) {
        assessment?.let { data ->
            val riskColor = data.riskLevel.color()
            val title = data.riskLevel.headline().uppercase()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OverlaySurface, RoundedCornerShape(20.dp))
                    .border(1.5.dp, riskColor.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
                    .semantics {
                        contentDescription = "Viora risk overlay: $title"
                    }
            ) {
                // ---- Header -------------------------------------------------
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⚠",
                        color = riskColor,
                        fontSize = 18.sp,
                        modifier = Modifier.size(width = 22.dp, height = 22.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = title,
                        color = riskColor, // bright accent on near-black surface: high contrast
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(riskColor, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ---- Payment summary line (e.g. "₹850 → random@upi") --------
                context?.let { ctx ->
                    val payee = ctx.upiId
                    if (payee != null) {
                        val currency = ctx.currency ?: "₹"
                        Text(
                            text = "${ctx.amount?.let { "$currency$it" } ?: "amount n/a"} → $payee",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                // ---- Reason lines (explainable signals) ---------------------
                val reasons = data.signals.take(2).map { it.description }
                    .ifEmpty { listOf(data.explanation) }
                reasons.forEach { reason ->
                    Text(
                        text = reason,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // ---- Actions -------------------------------------------------
                if (data.riskLevel != RiskLevel.SAFE) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        onDismissClick?.let {
                            TextButton(
                                onClick = it,
                                modifier = Modifier.height(44.dp)
                            ) {
                                Text(
                                    "Dismiss",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        onDetailsClick?.let {
                            TextButton(
                                onClick = it,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = VioraPrimaryCyan
                                ),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Text("[ View Details ]", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

