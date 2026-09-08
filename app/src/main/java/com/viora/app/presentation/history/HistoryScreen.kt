package com.viora.app.presentation.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.viora.app.domain.history.ThreatHistoryRecord
import com.viora.app.domain.threat.RiskLevel
import com.viora.app.presentation.components.RiskDangerousColor
import com.viora.app.presentation.components.RiskSafeColor
import com.viora.app.presentation.components.RiskSuspiciousColor
import com.viora.app.presentation.components.RiskVerifyColor
import com.viora.app.presentation.components.VioraDarkBackground
import com.viora.app.presentation.components.VioraDarkSurface
import com.viora.app.presentation.components.VioraPrimaryCyan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "CHECK HISTORY",
                color = VioraPrimaryCyan,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (history.isEmpty()) {
                EmptyHistoryCard(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(history, key = { it.id }) { record ->
                        HistoryEntryCard(record)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VioraPrimaryCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Back to Home", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = VioraDarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "No checks yet",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Past scans and safety checks will be stored and viewable here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun HistoryEntryCard(record: ThreatHistoryRecord) {
    val riskColor = record.riskLevel.color()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = VioraDarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(riskColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = record.riskLevel.name,
                    color = riskColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatTimestamp(record.timestamp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = record.explanation,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            (record.merchantName ?: record.upiId)?.let { recipient ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Recipient: $recipient",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun formatTimestamp(epochMillis: Long): String =
    SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(epochMillis))

private fun RiskLevel.color(): Color = when (this) {
    RiskLevel.SAFE -> RiskSafeColor
    RiskLevel.VERIFY -> RiskVerifyColor
    RiskLevel.SUSPICIOUS -> RiskSuspiciousColor
    RiskLevel.DANGEROUS -> RiskDangerousColor
}
