package com.viora.app.presentation.components

import androidx.compose.ui.graphics.Color
import com.viora.app.domain.threat.RiskLevel

/**
 * Single source of truth for how a [RiskLevel] is presented — color, short label,
 * and headline copy. Previously duplicated (with drifting wording) across
 * ResultScreen, HistoryScreen and VioraRiskOverlay; centralizing here means every
 * screen communicates risk identically, per Phase 6's design-system requirement.
 *
 * Pure presentation only — never derives or alters risk itself. Reserved for risk
 * communication: these four colors are not reused for other UI elements.
 */
fun RiskLevel.color(): Color = when (this) {
    RiskLevel.SAFE -> RiskSafeColor
    RiskLevel.VERIFY -> RiskVerifyColor
    RiskLevel.SUSPICIOUS -> RiskSuspiciousColor
    RiskLevel.DANGEROUS -> RiskDangerousColor
}

/** Short chip/badge label, e.g. for the History list and Home's recent checks. */
fun RiskLevel.shortLabel(): String = name

/** Headline copy for the overlay/result — one clear sentence naming the state. */
fun RiskLevel.headline(): String = when (this) {
    RiskLevel.SAFE -> "No Issues Detected"
    RiskLevel.VERIFY -> "Verify Before Paying"
    RiskLevel.SUSPICIOUS -> "Suspicious Payment"
    RiskLevel.DANGEROUS -> "High Risk — Do Not Pay"
}
