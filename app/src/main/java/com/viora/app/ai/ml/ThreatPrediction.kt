package com.viora.app.ai.ml

enum class PredictionLabel { SCAM_LANGUAGE, BENIGN }

/**
 * Output of [LocalThreatClassifier]. [confidence] is the model's own raw
 * sigmoid probability of [PredictionLabel.SCAM_LANGUAGE] — the genuine value
 * the trained weights produce for the given input, never fabricated or
 * adjusted after the fact.
 */
data class ThreatPrediction(
    val label: PredictionLabel,
    val confidence: Float
)
