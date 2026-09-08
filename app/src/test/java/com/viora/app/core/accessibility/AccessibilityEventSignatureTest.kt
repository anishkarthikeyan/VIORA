package com.viora.app.core.accessibility

import com.viora.app.domain.threat.RiskLevel
import com.viora.app.domain.threat.ThreatAssessment
import com.viora.app.domain.threat.ThreatSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Phase 7 — duplicate-persistence/overlay protection for the accessibility flow.
 * The debounce in VioraAccessibilityService re-triggers analysis on every
 * visible-text hash change, which can fire repeatedly for one lingering screen
 * (a ticking timestamp, a blinking cursor) even though the underlying scam
 * content hasn't changed. These tests verify the signature that dedup relies on.
 */
class AccessibilityEventSignatureTest {

    private val assessment = ThreatAssessment(
        score = 75,
        riskLevel = RiskLevel.DANGEROUS,
        signals = listOf(
            ThreatSignal("ACCOUNT_THREAT", 50, RiskLevel.SUSPICIOUS, "desc"),
            ThreatSignal("URGENT_ACTION", 20, RiskLevel.VERIFY, "desc")
        ),
        explanation = "explanation",
        recommendedAction = "action"
    )

    @Test
    fun `same package and assessment produce the identical signature`() {
        val first = accessibilityEventSignature("com.whatsapp", assessment)
        val second = accessibilityEventSignature("com.whatsapp", assessment.copy())

        assertEquals(first, second)
    }

    @Test
    fun `a different package changes the signature`() {
        assertNotEquals(
            accessibilityEventSignature("com.whatsapp", assessment),
            accessibilityEventSignature("com.google.android.apps.messaging", assessment)
        )
    }

    @Test
    fun `a different risk level changes the signature`() {
        assertNotEquals(
            accessibilityEventSignature("com.whatsapp", assessment),
            accessibilityEventSignature("com.whatsapp", assessment.copy(riskLevel = RiskLevel.SUSPICIOUS))
        )
    }

    @Test
    fun `a different signal set changes the signature`() {
        val fewerSignals = assessment.copy(signals = assessment.signals.take(1))

        assertNotEquals(
            accessibilityEventSignature("com.whatsapp", assessment),
            accessibilityEventSignature("com.whatsapp", fewerSignals)
        )
    }
}
