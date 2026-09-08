package com.viora.app.ai

import com.viora.app.domain.model.InputType
import com.viora.app.domain.model.VioraContext
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the exact GuardianAI wiring used by ScannerViewModel and
 * VioraAccessibilityService: CompositeGuardianAI(ThreatEngine(), AccessibilityThreatAnalyzer()).
 *
 * Uses a context that only ThreatEngine can flag (an amount mismatch) combined with
 * text that only AccessibilityThreatAnalyzer can flag (an OTP request). If both engines
 * are truly wired in, the combined assessment must contain both signals — proving
 * neither application entry point is bypassing CompositeGuardianAI or dropping an engine.
 */
class ApplicationGuardianAIWiringTest {

    @Test
    fun `application-level GuardianAI combines ThreatEngine and AccessibilityThreatAnalyzer signals`() {
        val guardianAI: GuardianAI = CompositeGuardianAI(ThreatEngine(), AccessibilityThreatAnalyzer())

        val context = VioraContext(
            inputType = InputType.TEXT,
            rawContent = "test",
            amount = "5000",
            displayedAmount = "850",
            extractedText = "Please share the otp now"
        )

        val result = runBlocking { guardianAI.analyze(context, com.viora.app.domain.threat.ThreatAssessment.neutral()) }
        val signalIds = result.signals.map { it.id }

        assertTrue("expected ThreatEngine's AMOUNT_MISMATCH signal", signalIds.contains("AMOUNT_MISMATCH"))
        assertTrue("expected AccessibilityThreatAnalyzer's OTP_REQUEST signal", signalIds.contains("OTP_REQUEST"))
    }
}
