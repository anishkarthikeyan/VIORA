package com.viora.app.ai

import com.viora.app.ai.ml.LocalThreatClassifier
import com.viora.app.ai.ml.PredictionLabel
import com.viora.app.ai.ml.ThreatClassifier
import com.viora.app.ai.ml.ThreatPrediction
import com.viora.app.domain.model.VioraContext
import com.viora.app.domain.threat.RiskLevel
import com.viora.app.domain.threat.ThreatAssessment
import com.viora.app.domain.threat.ThreatSignal

/**
 * Phase 8 — augments (never replaces) [ThreatEngine]/[AccessibilityThreatAnalyzer]
 * with ONE additional, conservative signal from an on-device text classifier
 * (see [LocalThreatClassifier]): a genuinely trained bag-of-words logistic
 * regression, not a rules-based signal wearing an "ML" label.
 *
 * Contributes [ML_SCAM_LANGUAGE_ID] only when:
 *  - there is enough free-form text to classify at all ([modelInputText]
 *    returns null otherwise — no new/sensitive data is collected for this), and
 *  - the model's own confidence clears [CONFIDENCE_THRESHOLD].
 *
 * Never reports certainty ("AI says this is fraud") and never fabricates a
 * result: on ANY failure — classifier unavailable, exception, invalid output —
 * this returns a signal-free assessment. The deterministic engines are
 * unaffected either way; this class cannot change their scores or thresholds.
 */
class MLGuardianAI(
    private val classifier: ThreatClassifier
) : GuardianAI {

    override suspend fun analyze(
        context: VioraContext,
        assessment: ThreatAssessment
    ): ThreatAssessment = try {
        analyzeInternal(context)
    } catch (e: Exception) {
        // CompositeGuardianAI.flatMap's every engine's signals in one pass — an
        // uncaught exception here would silently drop ThreatEngine's and
        // AccessibilityThreatAnalyzer's signals too, not just this one. This
        // outer guard (on top of the inner classifier try/catch below) makes
        // that structurally impossible: MLGuardianAI can only ever contribute
        // nothing, never break the rest of the pipeline.
        emptyAssessment()
    }

    private suspend fun analyzeInternal(context: VioraContext): ThreatAssessment {
        val prediction = modelInputText(context)?.let { text ->
            try {
                classifier.predict(text)
            } catch (e: Exception) {
                // Never let a classifier failure escape MLGuardianAI or crash the caller.
                null
            }
        }

        val signals = if (isConfidentScamPrediction(prediction)) {
            listOf(
                ThreatSignal(
                    id = ML_SCAM_LANGUAGE_ID,
                    score = SIGNAL_SCORE,
                    severity = RiskLevel.VERIFY,
                    description = "On-device analysis detected language patterns commonly " +
                        "associated with scam or social-engineering messages."
                )
            )
        } else {
            emptyList()
        }

        val totalScore = signals.sumOf { it.score }.coerceIn(0, 100)
        val level = levelFor(totalScore)
        return ThreatAssessment(
            score = totalScore,
            riskLevel = level,
            signals = signals,
            explanation = if (signals.isEmpty()) {
                "On-device language analysis found no scam-pattern signal."
            } else {
                "${level.name}: " + signals.joinToString(" ") { it.description }
            },
            recommendedAction = if (signals.isEmpty()) "" else "Verify this message independently before acting."
        )
    }

    private fun isConfidentScamPrediction(prediction: ThreatPrediction?): Boolean {
        if (prediction == null) return false
        if (prediction.label != PredictionLabel.SCAM_LANGUAGE) return false
        if (prediction.confidence.isNaN() || prediction.confidence.isInfinite()) return false
        if (prediction.confidence < 0f || prediction.confidence > 1f) return false
        return prediction.confidence >= CONFIDENCE_THRESHOLD
    }

    /**
     * Minimum useful information only (Phase 8 §5): free-form language fields
     * that can actually contain scam phrasing. Deliberately excludes
     * merchant/URL/source-app fields — those aren't "language" a text
     * classifier reasons about, and using them would collect more than needed.
     * Nothing here is persisted; the string is used in-memory for this one
     * classification and discarded.
     */
    private fun modelInputText(context: VioraContext): String? {
        val combined = listOfNotNull(context.note, context.extractedText)
            .joinToString(" ")
            .trim()
        return combined.ifEmpty { null }
    }

    private fun emptyAssessment(): ThreatAssessment = ThreatAssessment(
        score = 0,
        riskLevel = RiskLevel.SAFE,
        signals = emptyList(),
        explanation = "On-device language analysis found no scam-pattern signal.",
        recommendedAction = ""
    )

    private fun levelFor(score: Int): RiskLevel = when {
        score >= DANGEROUS_THRESHOLD -> RiskLevel.DANGEROUS
        score >= SUSPICIOUS_THRESHOLD -> RiskLevel.SUSPICIOUS
        score >= VERIFY_THRESHOLD -> RiskLevel.VERIFY
        else -> RiskLevel.SAFE
    }

    companion object {
        const val ML_SCAM_LANGUAGE_ID = "ML_SCAM_LANGUAGE"

        /**
         * Chosen conservatively from the held-out UCI SMS Spam Collection test
         * split (see scripts/ml/train.py / trainingMetadata in the shipped
         * asset): at 0.75, adjacent benign phrasing tried during training
         * (bank statements, appointment reminders, OTP notices, ordinary
         * "account"/"urgent"/"verify" usage) scored well below threshold
         * (≤0.26), while the target scam pattern ("Your KYC has expired...")
         * scored ~0.80. Kept high on purpose — ML must not become a source of
         * noisy warnings alongside the deterministic engines.
         */
        private const val CONFIDENCE_THRESHOLD = 0.75f

        /**
         * Modest, additive score — equal to ThreatEngine's weakest existing
         * signal (UNKNOWN_RECIPIENT = 20, VERIFY). On its own, ML never pushes
         * risk past VERIFY; combined with deterministic signals it
         * appropriately raises the total score. Existing ThreatEngine/
         * AccessibilityThreatAnalyzer signal weights are unchanged by this.
         */
        private const val SIGNAL_SCORE = 20

        // Mirrors the same thresholds ThreatEngine/AccessibilityThreatAnalyzer/
        // CompositeGuardianAI already use, so a standalone MLGuardianAI
        // assessment classifies consistently with the rest of the system.
        private const val VERIFY_THRESHOLD = 20
        private const val SUSPICIOUS_THRESHOLD = 50
        private const val DANGEROUS_THRESHOLD = 75
    }
}
