package com.viora.app.ai

import com.viora.app.ai.ml.PredictionLabel
import com.viora.app.ai.ml.ThreatClassifier
import com.viora.app.ai.ml.ThreatPrediction
import com.viora.app.domain.model.InputType
import com.viora.app.domain.model.VioraContext
import com.viora.app.domain.threat.RiskLevel
import com.viora.app.domain.threat.ThreatAssessment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 8 — MLGuardianAI unit tests. Uses a fake [ThreatClassifier] (no
 * Android/Robolectric needed) so this stays a small, focused unit-test suite,
 * not a large integration harness.
 */
class MLGuardianAITest {

    private val context = VioraContext(
        inputType = InputType.TEXT,
        rawContent = "raw",
        extractedText = "Your KYC has expired. Pay Rs 999 immediately or your account will be blocked."
    )

    private fun analyze(classifier: ThreatClassifier, ctx: VioraContext = context): ThreatAssessment =
        runBlocking { MLGuardianAI(classifier).analyze(ctx, ThreatAssessment.neutral()) }

    private fun fixedClassifier(prediction: ThreatPrediction?): ThreatClassifier =
        object : ThreatClassifier {
            override fun predict(text: String): ThreatPrediction? = prediction
        }

    // 1. high-confidence scam prediction produces ML signal
    @Test
    fun `high-confidence scam prediction produces the ML signal`() {
        val result = analyze(fixedClassifier(ThreatPrediction(PredictionLabel.SCAM_LANGUAGE, 0.90f)))

        assertEquals(1, result.signals.size)
        assertEquals("ML_SCAM_LANGUAGE", result.signals.first().id)
        assertEquals(RiskLevel.VERIFY, result.riskLevel)
        assertTrue(result.score > 0)
    }

    // 2. benign prediction produces no ML signal
    @Test
    fun `benign prediction produces no ML signal`() {
        val result = analyze(fixedClassifier(ThreatPrediction(PredictionLabel.BENIGN, 0.95f)))

        assertTrue(result.signals.isEmpty())
        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertEquals(0, result.score)
    }

    // 3. low-confidence prediction produces no ML signal
    @Test
    fun `low-confidence scam prediction produces no ML signal`() {
        // Below MLGuardianAI's conservative 0.75 threshold.
        val result = analyze(fixedClassifier(ThreatPrediction(PredictionLabel.SCAM_LANGUAGE, 0.60f)))

        assertTrue(result.signals.isEmpty())
        assertEquals(RiskLevel.SAFE, result.riskLevel)
    }

    // 4. missing text produces no ML signal
    @Test
    fun `missing text produces no ML signal without ever calling the classifier`() {
        var called = false
        val classifier = object : ThreatClassifier {
            override fun predict(text: String): ThreatPrediction? {
                called = true
                return ThreatPrediction(PredictionLabel.SCAM_LANGUAGE, 0.99f)
            }
        }
        val emptyContext = VioraContext(inputType = InputType.TEXT, rawContent = "")

        val result = analyze(classifier, emptyContext)

        assertTrue(result.signals.isEmpty())
        assertTrue("classifier should not run with no usable text", !called)
    }

    // 5. invalid prediction produces no ML signal
    @Test
    fun `NaN confidence produces no ML signal`() {
        val result = analyze(fixedClassifier(ThreatPrediction(PredictionLabel.SCAM_LANGUAGE, Float.NaN)))
        assertTrue(result.signals.isEmpty())
    }

    @Test
    fun `out-of-range confidence produces no ML signal`() {
        val result = analyze(fixedClassifier(ThreatPrediction(PredictionLabel.SCAM_LANGUAGE, 1.5f)))
        assertTrue(result.signals.isEmpty())
    }

    @Test
    fun `null prediction (classifier unavailable) produces no ML signal`() {
        val result = analyze(fixedClassifier(null))
        assertTrue(result.signals.isEmpty())
        assertEquals(RiskLevel.SAFE, result.riskLevel)
    }

    // 6. classifier exception does not escape MLGuardianAI
    @Test
    fun `classifier exception does not escape MLGuardianAI`() {
        val throwingClassifier = object : ThreatClassifier {
            override fun predict(text: String): ThreatPrediction? =
                throw IllegalStateException("simulated inference failure")
        }

        val result = analyze(throwingClassifier)

        assertTrue(result.signals.isEmpty())
        assertEquals(RiskLevel.SAFE, result.riskLevel)
    }

    @Test
    fun `never produces more than one signal from one prediction`() {
        val result = analyze(fixedClassifier(ThreatPrediction(PredictionLabel.SCAM_LANGUAGE, 0.99f)))
        assertTrue(result.signals.size <= 1)
    }
}
