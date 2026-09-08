package com.viora.app.domain.history

import com.viora.app.domain.model.InputType
import com.viora.app.domain.model.VioraContext
import com.viora.app.domain.threat.RiskLevel
import com.viora.app.domain.threat.ThreatAssessment
import com.viora.app.domain.threat.ThreatSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Phase 7 — duplicate-persistence protection. ScannerViewModel's continuous
 * camera stream (OCR re-fires every ~2s even when the scene hasn't changed —
 * see OcrFrameAnalyzer) would otherwise re-persist an unchanged result on every
 * pass. These tests verify the signature the dedup guard relies on.
 */
class SecurityEventSignatureTest {

    private val context = VioraContext(
        inputType = InputType.QR,
        rawContent = "upi://pay?pa=merchant@upi&pn=Corner Store&am=500",
        upiId = "merchant@upi",
        merchantName = "Corner Store",
        amount = "500",
        currency = "INR",
        visibleMerchant = "Corner Store",
        displayedAmount = "500"
    )

    private val assessment = ThreatAssessment(
        score = 25,
        riskLevel = RiskLevel.VERIFY,
        signals = listOf(ThreatSignal("UNKNOWN_RECIPIENT", 20, RiskLevel.VERIFY, "desc")),
        explanation = "explanation",
        recommendedAction = "action"
    )

    @Test
    fun `identical context and assessment produce the identical signature`() {
        // Simulates the same QR + unchanged scene being re-analyzed by a repeat
        // OCR pass: analysis re-runs, but the outcome — and so the signature — is
        // the same, which is exactly what must be deduped.
        val first = securityEventSignature(context, assessment)
        val second = securityEventSignature(context.copy(), assessment.copy())

        assertEquals(first, second)
    }

    @Test
    fun `a different amount changes the signature`() {
        val changed = context.copy(amount = "50000")

        assertNotEquals(
            securityEventSignature(context, assessment),
            securityEventSignature(changed, assessment)
        )
    }

    @Test
    fun `a different merchant changes the signature`() {
        val changed = context.copy(visibleMerchant = "Sunrise Bakery")

        assertNotEquals(
            securityEventSignature(context, assessment),
            securityEventSignature(changed, assessment)
        )
    }

    @Test
    fun `a different risk level changes the signature even with the same context`() {
        val escalated = assessment.copy(riskLevel = RiskLevel.DANGEROUS)

        assertNotEquals(
            securityEventSignature(context, assessment),
            securityEventSignature(context, escalated)
        )
    }

    @Test
    fun `a different signal set changes the signature`() {
        val differentSignals = assessment.copy(
            signals = listOf(ThreatSignal("AMOUNT_MISMATCH", 35, RiskLevel.SUSPICIOUS, "desc"))
        )

        assertNotEquals(
            securityEventSignature(context, assessment),
            securityEventSignature(context, differentSignals)
        )
    }
}
