package com.viora.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viora.app.core.accessibility.AccessibilityPermissionHelper
import com.viora.app.presentation.components.RiskBadge
import com.viora.app.presentation.components.RiskSafeColor
import com.viora.app.presentation.components.RiskSuspiciousColor
import com.viora.app.presentation.components.SectionHeader
import com.viora.app.presentation.components.VioraCard
import com.viora.app.presentation.components.VioraDarkBackground
import com.viora.app.presentation.components.VioraEmptyState
import com.viora.app.presentation.components.VioraPrimaryCyan
import com.viora.app.presentation.components.VioraRadius
import com.viora.app.presentation.components.VioraSpacing

@Composable
fun HomeScreen(
    onCheckWithVioraClick: () -> Unit,
    onCheckLinkMessageClick: () -> Unit,
    onCheckScreenshotImageClick: () -> Unit,
    onViewHistoryClick: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val recentChecks by viewModel.recentChecks.collectAsStateWithLifecycle()

    // Accessibility enablement can only change in system Settings, so re-check
    // whenever Home returns to the foreground (e.g. after the user grants it there).
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var protectionEnabled by remember {
        mutableStateOf(AccessibilityPermissionHelper.isEnabled(context))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                protectionEnabled = AccessibilityPermissionHelper.isEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                text = "VIORA",
                color = VioraPrimaryCyan,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Payment Security Intelligence",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(VioraSpacing.lg))

            ProtectionStatusRow(
                enabled = protectionEnabled,
                onEnableClick = { AccessibilityPermissionHelper.openSettings(context) }
            )

            Spacer(modifier = Modifier.height(VioraSpacing.lg))

            // Primary Call to Action
            Button(
                onClick = onCheckWithVioraClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(VioraRadius.lg),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VioraPrimaryCyan,
                    contentColor = Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "SCAN PAYMENT",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(VioraSpacing.md))

            // Secondary Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VioraSpacing.md)
            ) {
                OutlinedButton(
                    onClick = onCheckLinkMessageClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(VioraRadius.sm)
                ) {
                    Text(
                        text = "Link / Message",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = onCheckScreenshotImageClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(VioraRadius.sm)
                ) {
                    Text(
                        text = "Screenshot",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(VioraSpacing.xxl))

            // Recent Checks Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Checks",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (recentChecks.isNotEmpty()) {
                    Text(
                        text = "View All",
                        color = VioraPrimaryCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onViewHistoryClick() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(VioraSpacing.md))

            if (recentChecks.isEmpty()) {
                VioraEmptyState(
                    title = "No checks yet",
                    subtitle = "Scan a payment QR or share a suspicious message — " +
                        "your results will show up here.",
                    modifier = Modifier.clickable { onViewHistoryClick() }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(VioraSpacing.md)
                ) {
                    items(recentChecks) { item ->
                        RecentCheckRow(item, onClick = onViewHistoryClick)
                    }
                }
            }
        }
    }
}

/** Tapping a preview row opens the full History list — no separate detail screen exists or is needed. */
@Composable
private fun RecentCheckRow(item: RecentCheckItem, onClick: () -> Unit) {
    VioraCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.width(VioraSpacing.sm))
            RiskBadge(riskLevel = item.riskLevel, fontSize = 12.sp, dotSize = 8.dp)
        }
    }
}

/**
 * Real accessibility-permission status (Phase 5) — Viora never silently enables
 * background protection; this only reports whether the user already has, and
 * deep-links to Android's own Settings screen when they haven't.
 */
@Composable
private fun ProtectionStatusRow(enabled: Boolean, onEnableClick: () -> Unit) {
    VioraCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(if (enabled) RiskSafeColor else RiskSuspiciousColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(VioraSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                SectionHeader(if (enabled) "PROTECTION ACTIVE" else "PROTECTION OFF")
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (enabled) {
                        "Viora is watching payment and messaging apps for scams."
                    } else {
                        "Turn on background protection to get warned before you pay."
                    },
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
            if (!enabled) {
                Spacer(modifier = Modifier.width(VioraSpacing.sm))
                Text(
                    text = "Enable",
                    color = VioraPrimaryCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onEnableClick() }
                )
            }
        }
    }
}
