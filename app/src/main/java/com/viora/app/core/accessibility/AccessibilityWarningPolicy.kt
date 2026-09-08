package com.viora.app.core.accessibility

import com.viora.app.domain.threat.RiskLevel

/**
 * Decides whether a completed [com.viora.app.domain.threat.ThreatAssessment] warrants
 * interrupting the user with the proactive overlay while they are in another app.
 *
 * Uses the existing [RiskLevel] semantics as-is — no new thresholds or scores are
 * invented. The bar for a passive/ambient interruption is intentionally higher than
 * for an explicit, user-initiated scan result (ResultScreen shows every level,
 * including VERIFY): background monitoring must avoid alert fatigue (Phase 5's own
 * stated goal — "avoid: warning on every screen, warning on harmless text"), so only
 * SUSPICIOUS and DANGEROUS trigger the overlay. VERIFY-level findings are still
 * recorded to history (nothing is silently dropped) — just not interruptive.
 */
object AccessibilityWarningPolicy {
    /**
     * Anything above SAFE is worth keeping in history — nothing is silently
     * dropped — even when [shouldWarn] says it isn't strong enough to interrupt
     * the user. SAFE screens are never recorded: they carry no signals worth
     * explaining later and would otherwise flood history with routine app usage.
     */
    fun shouldRecord(riskLevel: RiskLevel): Boolean = riskLevel != RiskLevel.SAFE

    fun shouldWarn(riskLevel: RiskLevel): Boolean =
        riskLevel == RiskLevel.SUSPICIOUS || riskLevel == RiskLevel.DANGEROUS
}
