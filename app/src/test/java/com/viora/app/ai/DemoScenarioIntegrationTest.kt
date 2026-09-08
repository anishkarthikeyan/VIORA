package com.viora.app.ai

import com.viora.app.domain.fusion.ContextFusionEngine
import com.viora.app.domain.fusion.FusionInput
import com.viora.app.domain.model.InputType
import com.viora.app.domain.model.OcrResult
import com.viora.app.domain.model.VioraContext
import com.viora.app.domain.threat.RiskLevel
import com.viora.app.domain.threat.ThreatAssessment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 4 — "isolated test/demo mechanism" proving the three primary submission
 * demo scenarios through the REAL pipeline: the exact `CompositeGuardianAI(ThreatEngine(),
 * AccessibilityThreatAnalyzer())` wiring ScannerViewModel/VioraAccessibilityService use
 * by default, fed contexts shaped exactly like what QR+OCR fusion or a text share would
 * actually produce. This is deterministic, repeatable, and adds no demo-only code path
 * to the app itself — see DEMO_SCRIPT.md for how to reproduce each scenario live.
 */
class DemoScenarioIntegrationTest {

    private val guardianAI: GuardianAI = CompositeGuardianAI(ThreatEngine(), AccessibilityThreatAnalyzer())
    private val fusionEngine = ContextFusionEngine()

    private fun analyze(context: VioraContext): ThreatAssessment =
        runBlocking { guardianAI.analyze(context, ThreatAssessment.neutral()) }

    /**
     * Scenario A — Amount manipulation: QR payload requests ₹50,000 while the scene
     * (OCR) shows ₹500 displayed on screen. Mirrors exactly what QrFrameAnalyzer +
     * OcrFrameAnalyzer + ContextFusionEngine hand to GuardianAI in the real scanner flow.
     */
    @Test
    fun `scenario A - amount manipulation raises AMOUNT_MISMATCH`() {
        val qrPayload = VioraContext(
            inputType = InputType.QR,
            rawContent = "upi://pay?pa=merchant@upi&pn=Corner%20Store&am=50000&cu=INR",
            upiId = "merchant@upi",
            merchantName = "Corner Store",
            amount = "50000",
            currency = "INR"
        )
        val scene = OcrResult(rawText = "Corner Store\nPay ₹500", lineCount = 2, detectedAtMs = 0L)

        val fused = fusionEngine.fuse(listOf(FusionInput.Parsed(qrPayload), FusionInput.SceneText(scene)))
        val assessment = analyze(fused)

        assertTrue(
            "expected AMOUNT_MISMATCH, got ${assessment.signals.map { it.id }}",
            assessment.signals.any { it.id == "AMOUNT_MISMATCH" }
        )
        assertTrue(assessment.riskLevel != RiskLevel.SAFE)
    }

    /**
     * Scenario B — Merchant mismatch: QR claims one business name while the scene
     * visibly shows a completely unrelated one.
     */
    @Test
    fun `scenario B - merchant mismatch raises MERCHANT_MISMATCH`() {
        val qrPayload = VioraContext(
            inputType = InputType.QR,
            rawContent = "upi://pay?pa=randomhandle123@upi&pn=Zenith%20Traders&am=1200&cu=INR",
            upiId = "randomhandle123@upi",
            merchantName = "Zenith Traders",
            amount = "1200",
            currency = "INR"
        )
        val scene = OcrResult(rawText = "Sunrise Bakery\nPay ₹1200", lineCount = 2, detectedAtMs = 0L)

        val fused = fusionEngine.fuse(listOf(FusionInput.Parsed(qrPayload), FusionInput.SceneText(scene)))
        val assessment = analyze(fused)

        assertTrue(
            "expected MERCHANT_MISMATCH, got ${assessment.signals.map { it.id }}",
            assessment.signals.any { it.id == "MERCHANT_MISMATCH" }
        )
        assertTrue(assessment.riskLevel != RiskLevel.SAFE)
    }

    /**
     * Scenario C — Social engineering: text shared into Viora (e.g. from an SMS/chat
     * app) claiming KYC expiry with urgency and a payment demand. Mirrors exactly what
     * ShareInputProcessor hands to GuardianAI for a plain-text share (extractedText set,
     * no QR/payload fields).
     */
    @Test
    fun `scenario C - social engineering text raises account-threat, urgency and payment signals`() {
        val sharedText = "KYC has expired. Pay Rs 999 immediately or your account will be blocked."
        val context = VioraContext(
            inputType = InputType.TEXT,
            rawContent = sharedText,
            extractedText = sharedText
        )

        val assessment = analyze(context)
        val signalIds = assessment.signals.map { it.id }.toSet()

        assertTrue("expected ACCOUNT_THREAT, got $signalIds", signalIds.contains("ACCOUNT_THREAT"))
        assertTrue("expected URGENT_ACTION, got $signalIds", signalIds.contains("URGENT_ACTION"))
        assertTrue("expected PAYMENT_REQUEST, got $signalIds", signalIds.contains("PAYMENT_REQUEST"))
        assertEquals(RiskLevel.DANGEROUS, assessment.riskLevel)
    }

    /** Sanity check: a clean, consistent QR payment produces no mismatch signals. */
    @Test
    fun `consistent payment produces no mismatch signals`() {
        val qrPayload = VioraContext(
            inputType = InputType.QR,
            rawContent = "upi://pay?pa=merchant@upi&pn=Corner%20Store&am=500&cu=INR",
            upiId = "merchant@upi",
            merchantName = "Corner Store",
            amount = "500",
            currency = "INR"
        )
        val scene = OcrResult(rawText = "Corner Store\nPay ₹500", lineCount = 2, detectedAtMs = 0L)

        val fused = fusionEngine.fuse(listOf(FusionInput.Parsed(qrPayload), FusionInput.SceneText(scene)))
        val assessment = analyze(fused)

        assertTrue(assessment.signals.none { it.id == "AMOUNT_MISMATCH" || it.id == "MERCHANT_MISMATCH" })
    }
}
