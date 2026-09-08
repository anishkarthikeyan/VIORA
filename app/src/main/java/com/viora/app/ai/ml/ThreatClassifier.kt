package com.viora.app.ai.ml

/**
 * Boundary between [MLGuardianAI][com.viora.app.ai.MLGuardianAI] and whatever
 * actually performs on-device inference. [LocalThreatClassifier] is the real,
 * shipped implementation; tests inject a fake/mock implementation instead of
 * needing Android/Robolectric to exercise MLGuardianAI's signal logic. Keeps
 * the model implementation itself replaceable, per Phase 8's architecture goal.
 */
interface ThreatClassifier {
    /** Returns a genuine prediction, or null if the model/input isn't usable. */
    fun predict(text: String): ThreatPrediction?
}
