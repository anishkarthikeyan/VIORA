package com.viora.app.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.viora.app.domain.history.ThreatHistoryRecord
import com.viora.app.presentation.components.RiskBadge
import com.viora.app.presentation.components.SecondaryActionButton
import com.viora.app.presentation.components.color
import com.viora.app.presentation.components.VioraCard
import com.viora.app.presentation.components.VioraDarkBackground
import com.viora.app.presentation.components.VioraEmptyState
import com.viora.app.presentation.components.VioraPrimaryCyan
import com.viora.app.presentation.components.VioraSpacing
import com.viora.app.presentation.components.signalLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Real Room-backed security-check history (Phase 3 data, Phase 6 presentation). */
@Composable
fun HistoryScreen(
    history: List<ThreatHistoryRecord> = emptyList(),
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
                text = "CHECK HISTORY",
                color = VioraPrimaryCyan,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(VioraSpacing.xs))
            Text(
                text = if (history.isEmpty()) {
                    "Every scan and background check Viora completes appears here."
                } else {
                    "${history.size} check${if (history.size == 1) "" else "s"}, newest first."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(VioraSpacing.lg))

            if (history.isEmpty()) {
                VioraEmptyState(
                    title = "No checks yet",
                    subtitle = "Scan a payment QR, share a suspicious message, or let " +
                        "background protection run — every completed check is stored here, on-device only.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(VioraSpacing.md)
                ) {
                    items(history, key = { it.id }) { record ->
                        HistoryEntryCard(record)
                    }
                }
            }

            Spacer(modifier = Modifier.height(VioraSpacing.lg))

            SecondaryActionButton(text = "Back to Home", onClick = onBackClick)
        }
    }
}

@Composable
private fun HistoryEntryCard(record: ThreatHistoryRecord) {
    VioraCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RiskBadge(riskLevel = record.riskLevel, modifier = Modifier.weight(1f))
            Text(
                text = formatTimestamp(record.timestamp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(VioraSpacing.sm))

        Text(
            text = record.explanation,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        val topSignal = record.signals.maxByOrNull { it.score }
        val recipient = record.merchantName ?: record.upiId
        if (topSignal != null || recipient != null) {
            Spacer(modifier = Modifier.height(VioraSpacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                topSignal?.let {
                    Text(
                        text = signalLabel(it.id),
                        color = record.riskLevel.color(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                if (topSignal != null && recipient != null) {
                    Spacer(modifier = Modifier.width(VioraSpacing.sm))
                    Text(text = "•", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(VioraSpacing.sm))
                }
                recipient?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(epochMillis: Long): String =
    SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(epochMillis))
