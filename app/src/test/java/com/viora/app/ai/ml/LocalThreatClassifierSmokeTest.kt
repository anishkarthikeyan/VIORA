package com.viora.app.ai.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Phase 8 — genuine smoke test of the REAL shipped model artifact
 * (`app/src/main/assets/ml/scam_language_classifier_v1.json`), not a fake
 * standing in for it. Reads the actual asset file from disk (no Android
 * Context/AssetManager/Robolectric needed — [LocalThreatClassifier.parseModel]/
 * [LocalThreatClassifier.predictWithModel] are pure) and runs the two
 * functional checks the Phase 8 plan asks for: a high-risk scam-style sentence
 * should score meaningfully higher than an ordinary payment sentence.
 *
 * Deliberately not a quantitative accuracy claim from two examples — see
 * `scripts/ml/train.py`'s held-out test metrics (recorded in the asset's own
 * `trainingMetadata`) for that.
 */
class LocalThreatClassifierSmokeTest {

    private val model: LocalThreatClassifier.Model by lazy {
        val file = File("src/main/assets/ml/scam_language_classifier_v1.json")
        assertTrue("shipped model asset must exist at ${file.path}", file.exists())
        LocalThreatClassifier.parseModel(file.readText())
    }

    @Test
    fun `real model loads with a non-empty vocabulary`() {
        assertTrue(model.vocabIndex.isNotEmpty())
        assertEquals(model.vocabIndex.size, model.weights.size)
    }

    @Test
    fun `real model flags the high-risk KYC scam sentence as SCAM_LANGUAGE`() {
        val prediction = LocalThreatClassifier.predictWithModel(
            model,
            "Your KYC has expired. Pay Rs 999 immediately or your account will be blocked."
        )

        assertTrue("expected a prediction, got null", prediction != null)
        assertEquals(PredictionLabel.SCAM_LANGUAGE, prediction!!.label)
    }

    @Test
    fun `real model does not flag an ordinary payment sentence as fraudulent`() {
        val prediction = LocalThreatClassifier.predictWithModel(
            model,
            "Send ₹500 to your friend for dinner."
        )

        // Functional smoke check only (per Phase 8 §17): the benign example
        // must not be classified as scam language.
        assertTrue(
            "benign payment text should not be classified as SCAM_LANGUAGE",
            prediction == null || prediction.label == PredictionLabel.BENIGN
        )
    }

    @Test
    fun `the scam sentence scores meaningfully higher than the benign one`() {
        val scam = LocalThreatClassifier.predictWithModel(
            model,
            "Your KYC has expired. Pay Rs 999 immediately or your account will be blocked."
        )
        val benign = LocalThreatClassifier.predictWithModel(
            model,
            "Send ₹500 to your friend for dinner."
        )

        assertTrue(scam != null && benign != null)
        assertTrue(
            "scam confidence (${scam!!.confidence}) should exceed benign confidence (${benign!!.confidence})",
            scam.confidence > benign.confidence
        )
    }
}
