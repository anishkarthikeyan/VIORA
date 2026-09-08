package com.viora.app.ai

import com.viora.app.domain.fusion.ContextFusionEngine
import com.viora.app.domain.fusion.FusionInput
import com.viora.app.domain.model.InputType
import com.viora.app.domain.model.OcrResult
import com.viora.app.domain.model.VioraContext
import com.viora.app.domain.parser.UpiParseResult
import com.viora.app.domain.parser.UpiParser
import com.viora.app.domain.threat.RiskLevel
import com.viora.app.domain.threat.ThreatAssessment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Screenshot QR + context gap fix — verifies the REAL data flow end to end:
 * UpiParser (QR payload) + ContextFusionEngine (QR + OCR) + the real
 * CompositeGuardianAI(ThreatEngine(), AccessibilityThreatAnalyzer()) production
 * wiring. Mirrors DemoScenarioIntegrationTest's style: no fakes standing in for
 * the actual decoding/parsing path — only the ML Kit calls themselves (Android-
 * coupled, exercised structurally via ScannerViewModel/SharedImageProcessor,
 * not unit-testable here) are outside this test's reach.
 */
class ScreenshotQrContextIntegrationTest {

    private val guardianAI: GuardianAI = CompositeGuardianAI(ThreatEngine(), AccessibilityThreatAnalyzer())
    private val fusionEngine = ContextFusionEngine()
    private val upiParser = UpiParser()

    private fun analyze(context: VioraContext): ThreatAssessment =
        runBlocking { guardianAI.analyze(context, ThreatAssessment.neutral()) }

    private fun ocr(text: String) = OcrResult(rawText = text, lineCount = text.lines().size, detectedAtMs = 0L)

    /** Mirrors ScannerViewModel.onSharedImage's own QR-decode-to-context step exactly. */
    private fun qrContextOf(rawUpiUri: String): VioraContext? =
        when (val parsed = upiParser.parse(rawUpiUri)) {
            is UpiParseResult.UpiPayment -> parsed.context
            is UpiParseResult.Url -> parsed.context
            is UpiParseResult.Text -> parsed.context
            is UpiParseResult.MalformedUpi -> null
        }

    // ---------- Canonical regression case (Phase task's provided screenshot) ----------

    @Test
    fun `canonical screenshot - QR payload is actually decoded and parsed`() {
        val qrContext = qrContextOf(
            "upi://pay?pa=nied.foundation@ybl&pn=Dr%20DHANRAJ&mc=0000&mode=02&purpose=00"
        )

        assertTrue("QR payload must parse into a usable context", qrContext != null)
        assertEquals("nied.foundation@ybl", qrContext!!.upiId)
        assertEquals("Dr DHANRAJ", qrContext.merchantName) // %20 decoded to a space
        assertNull("QR has no am — must not be invented", qrContext.amount)
    }

    @Test
    fun `canonical screenshot - fusion combines QR payload and visible OCR context`() {
        val qrContext = qrContextOf(
            "upi://pay?pa=nied.foundation@ybl&pn=Dr%20DHANRAJ&mc=0000&mode=02&purpose=00"
        )!!
        val scene = ocr("ALL UPI ACCEPTED\nNEXTGEN INDIA\nEDUCATIONAL DEVELOPMENT COUNCIL\nnied.foundation@ybl")

        val fused = fusionEngine.fuse(listOf(FusionInput.Parsed(qrContext), FusionInput.SceneText(scene)))

        // QR side
        assertEquals("nied.foundation@ybl", fused.upiId)
        assertEquals("Dr DHANRAJ", fused.merchantName)
        assertNull(fused.amount)
        // Visible (OCR) side
        assertEquals("NEXTGEN INDIA", fused.visibleMerchant)
        assertEquals("nied.foundation@ybl", fused.visibleUpiId)
        assertNull(fused.displayedAmount) // scene has no rendered amount either
    }

    @Test
    fun `canonical screenshot - UPI ID matches, organization name differs, no invented amount mismatch, no DANGEROUS scam label`() {
        val qrContext = qrContextOf(
            "upi://pay?pa=nied.foundation@ybl&pn=Dr%20DHANRAJ&mc=0000&mode=02&purpose=00"
        )!!
        val scene = ocr("ALL UPI ACCEPTED\nNEXTGEN INDIA\nEDUCATIONAL DEVELOPMENT COUNCIL\nnied.foundation@ybl")
        val fused = fusionEngine.fuse(listOf(FusionInput.Parsed(qrContext), FusionInput.SceneText(scene)))

        val result = analyze(fused)
        val signalIds = result.signals.map { it.id }.toSet()

        // UPI ID matches -> no false recipient-id mismatch.
        assertTrue("expected no RECIPIENT_ID_MISMATCH, got $signalIds", "RECIPIENT_ID_MISMATCH" !in signalIds)
        // QR has no amount -> must not fabricate an AMOUNT_MISMATCH.
        assertTrue("expected no AMOUNT_MISMATCH, got $signalIds", "AMOUNT_MISMATCH" !in signalIds)
        // Organization name vs QR payee name differ materially -> flagged, cautiously.
        assertTrue("expected MERCHANT_MISMATCH, got $signalIds", "MERCHANT_MISMATCH" in signalIds)
        assertTrue(
            "explanation should name both sides for the user, got: ${result.explanation}",
            result.explanation.contains("NEXTGEN INDIA") && result.explanation.contains("Dr DHANRAJ")
        )
        // Must stay cautious — a single soft name mismatch must not read as a confirmed scam.
        assertTrue(
            "risk must not escalate to DANGEROUS from a name mismatch alone, got ${result.riskLevel}",
            result.riskLevel == RiskLevel.VERIFY
        )
        assertTrue(
            "must not use absolute scam language, got: ${result.explanation}",
            !result.explanation.contains("SCAM", ignoreCase = true)
        )
    }

    // ---------- Step 6 test list ----------

    @Test
    fun `UPI QR with amount is parsed correctly`() {
        val qrContext = qrContextOf("upi://pay?pa=shop@upi&pn=Shop&am=4999&cu=INR")!!

        assertEquals("4999", qrContext.amount)
        assertEquals("INR", qrContext.currency)
    }

    @Test
    fun `high-value amount mismatch - visible 499 vs QR 4999`() {
        val qrContext = qrContextOf("upi://pay?pa=shop@upi&pn=Shop&am=4999")!!
        val scene = ocr("Shop\nPay ₹499")

        val fused = fusionEngine.fuse(listOf(FusionInput.Parsed(qrContext), FusionInput.SceneText(scene)))
        val result = analyze(fused)

        assertEquals("499", fused.displayedAmount)
        assertEquals("4999", fused.amount)
        assertTrue(result.signals.any { it.id == "AMOUNT_MISMATCH" })
        assertTrue(result.explanation.contains("499") && result.explanation.contains("4999"))
    }

    @Test
    fun `visible UPI ID differs from QR pa - mismatch available to risk analysis`() {
        val qrContext = qrContextOf("upi://pay?pa=attacker@upi&pn=Trusted%20Shop")!!
        val scene = ocr("Trusted Shop\nreal.shop@ybl")

        val fused = fusionEngine.fuse(listOf(FusionInput.Parsed(qrContext), FusionInput.SceneText(scene)))
        val result = analyze(fused)

        assertTrue(result.signals.any { it.id == "RECIPIENT_ID_MISMATCH" })
    }

    @Test
    fun `screenshot with no QR - OCR-only flow continues normally`() {
        // No FusionInput.Parsed at all — exactly what ScannerViewModel.onSharedImage
        // builds when SharedImageProcessor finds no barcode in the image.
        val scene = ocr("SUPER MART\nPay ₹999")

        val fused = fusionEngine.fuse(listOf(FusionInput.SceneText(scene)))
        val result = analyze(fused)

        assertEquals("SUPER MART", fused.visibleMerchant)
        assertNull(fused.upiId)
        // No payload to compare against -> no fabricated mismatch signals.
        assertTrue(result.signals.none { it.id == "AMOUNT_MISMATCH" || it.id == "MERCHANT_MISMATCH" })
    }

    @Test
    fun `screenshot with an invalid non-UPI QR does not crash and OCR still proceeds`() {
        // A barcode was detected but its content isn't a well-formed UPI URI —
        // UpiParser reports MalformedUpi; qrContextOf() must degrade to null, not throw.
        val qrContext = qrContextOf("upi://pay?pa=%zz")

        assertNull(qrContext)

        val scene = ocr("SUPER MART\nPay ₹999")
        val fused = fusionEngine.fuse(
            buildList {
                qrContext?.let { add(FusionInput.Parsed(it)) }
                add(FusionInput.SceneText(scene))
            }
        )
        val result = analyze(fused)

        assertEquals("SUPER MART", fused.visibleMerchant) // OCR flow unaffected
        assertTrue(result.riskLevel == RiskLevel.SAFE || result.riskLevel == RiskLevel.VERIFY)
    }

    @Test
    fun `screenshot QR that is a plain URL still contributes without crashing`() {
        val qrContext = qrContextOf("https://example.com/pay")

        assertTrue(qrContext != null)
        assertTrue(qrContext!!.detectedUrls.contains("https://example.com/pay"))
        assertNull(qrContext.upiId)
    }
}
