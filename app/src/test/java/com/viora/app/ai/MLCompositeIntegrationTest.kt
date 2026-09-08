package com.viora.app.ai

import com.viora.app.ai.ml.PredictionLabel
import com.viora.app.ai.ml.ThreatClassifier
import com.viora.app.ai.ml.ThreatPrediction
import com.viora.app.domain.model.InputType
import com.viora.app.domain.model.VioraContext
import com.viora.app.domain.threat.RiskLevel
import com.viora.app.domain.threat.ThreatAssessment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 8 — verifies MLGuardianAI plugs into the real application-level
 * CompositeGuardianAI(ThreatEngine(), AccessibilityThreatAnalyzer(), MLGuardianAI(...))
 * composition (same wiring as ScannerViewModelFactory/VioraAccessibilityService)
 * without disturbing the deterministic engines — ML is additive, and a broken
 * classifier must never take deterministic signals down with it.
 */
class MLCompositeIntegrationTest {

    private val scamText = "KYC has expired. Pay Rs 999 immediately or your account will be blocked."

    private fun fixedClassifier(prediction: ThreatPrediction?): ThreatClassifier =
        object : ThreatClassifier {
            override fun predict(text: String): ThreatPrediction? = prediction
        }

    private fun throwingClassifier(): ThreatClassifier = object : ThreatClassifier {
        override fun predict(text: String): ThreatPrediction? =
            throw IllegalStateException("simulated model failure")
    }

    private fun analyze(guardianAI: GuardianAI, context: VioraContext): ThreatAssessment =
        runBlocking { guardianAI.analyze(context, ThreatAssessment.neutral()) }

    // 7. CompositeGuardianAI combines deterministic + ML signals
    @Test
    fun `composite combines deterministic and ML signals`() {
        val guardianAI = CompositeGuardianAI(
            ThreatEngine(),
            AccessibilityThreatAnalyzer(),
            MLGuardianAI(fixedClassifier(ThreatPrediction(PredictionLabel.SCAM_LANGUAGE, 0.90f)))
        )
        val context = VioraContext(inputType = InputType.TEXT, rawContent = scamText, extractedText = scamText)

        val result = analyze(guardianAI, context)
        val signalIds = result.signals.map { it.id }.toSet()

        // Deterministic (AccessibilityThreatAnalyzer) signals still present...
        assertTrue("expected ACCOUNT_THREAT, got $signalIds", signalIds.contains("ACCOUNT_THREAT"))
        assertTrue("expected URGENT_ACTION, got $signalIds", signalIds.contains("URGENT_ACTION"))
        assertTrue("expected PAYMENT_REQUEST, got $signalIds", signalIds.contains("PAYMENT_REQUEST"))
        // ...AND the additive ML signal.
        assertTrue("expected ML_SCAM_LANGUAGE, got $signalIds", signalIds.contains("ML_SCAM_LANGUAGE"))
    }

    // 8. deterministic signals still work when ML fails
    @Test
    fun `deterministic signals survive when the ML classifier throws`() {
        val guardianAI = CompositeGuardianAI(
            ThreatEngine(),
            AccessibilityThreatAnalyzer(),
            MLGuardianAI(throwingClassifier())
        )
        val context = VioraContext(inputType = InputType.TEXT, rawContent = scamText, extractedText = scamText)

        val result = analyze(guardianAI, context)
        val signalIds = result.signals.map { it.id }.toSet()

        assertTrue("expected ACCOUNT_THREAT despite ML failure, got $signalIds", signalIds.contains("ACCOUNT_THREAT"))
        assertTrue("expected URGENT_ACTION despite ML failure, got $signalIds", signalIds.contains("URGENT_ACTION"))
        assertTrue("expected PAYMENT_REQUEST despite ML failure, got $signalIds", signalIds.contains("PAYMENT_REQUEST"))
        assertTrue("ML must not contribute a signal on failure", !signalIds.contains("ML_SCAM_LANGUAGE"))
        assertTrue(result.riskLevel == RiskLevel.DANGEROUS)
    }

    @Test
    fun `ML signal alone never exceeds VERIFY`() {
        val guardianAI = CompositeGuardianAI(
            ThreatEngine(),
            AccessibilityThreatAnalyzer(),
            MLGuardianAI(fixedClassifier(ThreatPrediction(PredictionLabel.SCAM_LANGUAGE, 0.99f)))
        )
        // Benign context with no deterministic signals — only ML would fire.
        val context = VioraContext(inputType = InputType.TEXT, rawContent = "hello", extractedText = "hello there")

        val result = analyze(guardianAI, context)

        assertTrue(result.riskLevel == RiskLevel.SAFE || result.riskLevel == RiskLevel.VERIFY)
    }
}
