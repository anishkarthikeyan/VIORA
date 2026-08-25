package com.viora.app.presentation.home

import androidx.lifecycle.ViewModel
import com.viora.app.domain.threat.RiskLevel
import com.viora.app.presentation.components.RiskSafeColor
import com.viora.app.presentation.components.RiskSuspiciousColor
import com.viora.app.presentation.components.RiskVerifyColor
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RecentCheckItem(
    val title: String,
    val subtitle: String,
    val statusLabel: String,
    val statusColor: Color,
    val riskLevel: RiskLevel
)

/**
 * Phase 1: mock data only. A real repository-backed history flow arrives in a later phase.
 */
class HomeViewModel : ViewModel() {

    private val _recentChecks = MutableStateFlow(mockChecks())
    val recentChecks: StateFlow<List<RecentCheckItem>> = _recentChecks.asStateFlow()

    private fun mockChecks(): List<RecentCheckItem> = listOf(
        RecentCheckItem("UPI QR Scan", "merchant@okicici (₹499)", "SAFE", RiskSafeColor, RiskLevel.SAFE),
        RecentCheckItem("SMS Link", "bit.ly/secure-bank-login", "SUSPICIOUS", RiskSuspiciousColor, RiskLevel.SUSPICIOUS),
        RecentCheckItem("Payment Link", "upi://pay?pa=store@upi", "VERIFY", RiskVerifyColor, RiskLevel.VERIFY)
    )
}
