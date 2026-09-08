package com.viora.app.ai.ml

import android.content.Context
import android.util.Log
import com.viora.app.BuildConfig
import kotlin.math.exp

/**
 * On-device bag-of-words logistic-regression classifier for scam/social-
 * engineering language.
 *
 * REAL, genuinely trained model — not regex/keyword rules relabeled as "ML".
 * See `scripts/ml/train.py` (not shipped in the APK): bag-of-words logistic
 * regression trained with gradient descent on the UCI SMS Spam Collection
 * (5,572 real SMS messages) plus a small, disclosed set of hand-written
 * domain-adaptation examples for Indian UPI/banking scam vocabulary. Held-out
 * test accuracy/precision/recall are recorded in the shipped asset's own
 * `trainingMetadata` field — reported honestly, never fabricated.
 *
 * The model is small and linear, so on-device inference is plain arithmetic
 * (dot product + sigmoid) against weights loaded from a bundled JSON asset —
 * no TensorFlow Lite/ONNX runtime needed, so this adds zero new Gradle
 * dependencies and zero native-library/APK-size/ABI risk.
 *
 * Fails safe: any problem at all (missing/malformed asset, empty vocabulary,
 * inference exception, NaN/out-of-range output) yields `null`. Callers MUST
 * treat `null` as "no ML signal available" — never as SAFE, never as a
 * fabricated verdict.
 */
class LocalThreatClassifier(private val context: Context) : ThreatClassifier {

    // Parsed at most once, on first use — never reloaded per frame/event
    // (ScannerViewModel/VioraAccessibilityService construct one long-lived
    // GuardianAI, so this instance — and this lazy — is reused across calls).
    private val model: Model? by lazy {
        runCatching {
            val json = context.assets.open(ASSET_PATH).use { it.reader().readText() }
            parseModel(json)
        }.getOrNull()
    }

    override fun predict(text: String): ThreatPrediction? {
        val m = model ?: return null
        return try {
            predictWithModel(m, text)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Inference failed: ${e.javaClass.simpleName}")
            null
        }
    }

    internal data class Model(
        val vocabIndex: Map<String, Int>,
        val weights: DoubleArray,
        val bias: Double
    )

    companion object {
        private const val TAG = "LocalThreatClassifier"
        private const val ASSET_PATH = "ml/scam_language_classifier_v1.json"

        /** Below this many distinct tokens there isn't enough text to classify meaningfully. */
        private const val MIN_TOKENS = 3

        private val TOKEN_REGEX = Regex("[a-z0-9]+")

        /** Vocabulary tokens are always plain `[a-z0-9]+` (see scripts/ml/train.py's tokenizer) — no escaping needed. */
        private val STRING_REGEX = Regex("\"([a-z0-9]*)\"")
        private val NUMBER_REGEX = Regex("-?\\d+(?:\\.\\d+)?(?:[eE][-+]?\\d+)?")

        /**
         * Pure model parsing — no Context/AssetManager, so it's directly
         * unit-testable against the real shipped JSON asset (read as a plain
         * file in tests) as a genuine smoke test of the actual model, not a
         * fake standing in for it.
         *
         * Hand-rolled rather than `org.json` deliberately: Android's unit-test
         * `android.jar` stub silently no-ops `org.json.JSONObject` (methods
         * return null instead of parsing) outside a real device/emulator or
         * Robolectric, which this project avoids per its testing conventions.
         * The model JSON has a fixed, simple, self-produced shape (flat string
         * array, flat number array, one scalar) — trivial and safe to parse
         * directly, on-device and in tests alike, with no JSON library needed.
         */
        internal fun parseModel(json: String): Model {
            val vocabulary = extractStringArray(json, "vocabulary")
            val weightsList = extractNumberArray(json, "weights")
            check(vocabulary.isNotEmpty()) { "empty vocabulary" }
            check(vocabulary.size == weightsList.size) { "vocabulary/weights size mismatch" }

            val vocabIndex = HashMap<String, Int>(vocabulary.size)
            vocabulary.forEachIndexed { i, token -> vocabIndex[token] = i }
            return Model(vocabIndex = vocabIndex, weights = weightsList.toDoubleArray(), bias = extractNumber(json, "bias"))
        }

        private fun extractStringArray(json: String, key: String): List<String> =
            STRING_REGEX.findAll(arrayContent(json, key)).map { it.groupValues[1] }.toList()

        private fun extractNumberArray(json: String, key: String): List<Double> =
            NUMBER_REGEX.findAll(arrayContent(json, key)).map { it.value.toDouble() }.toList()

        private fun extractNumber(json: String, key: String): Double {
            val match = Regex("\"${Regex.escape(key)}\"\\s*:\\s*(${NUMBER_REGEX.pattern})").find(json)
                ?: error("key \"$key\" not found")
            return match.groupValues[1].toDouble()
        }

        /** Content between the `[` and matching `]` immediately following `"key":`. */
        private fun arrayContent(json: String, key: String): String {
            val keyIndex = json.indexOf("\"$key\"")
            check(keyIndex >= 0) { "key \"$key\" not found" }
            val start = json.indexOf('[', keyIndex)
            check(start >= 0) { "array start not found for \"$key\"" }

            var depth = 0
            var inString = false
            var i = start
            while (i < json.length) {
                val c = json[i]
                when {
                    c == '"' && json.getOrNull(i - 1) != '\\' -> inString = !inString
                    !inString && c == '[' -> depth++
                    !inString && c == ']' -> {
                        depth--
                        if (depth == 0) return json.substring(start + 1, i)
                    }
                }
                i++
            }
            error("array end not found for \"$key\"")
        }

        /** Pure inference — no Context/AssetManager — shared by [predict] and tests. */
        internal fun predictWithModel(m: Model, text: String): ThreatPrediction? {
            val tokens = tokenize(text)
            if (tokens.size < MIN_TOKENS) return null

            var z = m.bias
            for (tok in tokens) {
                val idx = m.vocabIndex[tok] ?: continue
                z += m.weights[idx]
            }
            val p = sigmoid(z)
            if (p.isNaN() || p.isInfinite() || p < 0.0 || p > 1.0) return null

            val label = if (p >= 0.5) PredictionLabel.SCAM_LANGUAGE else PredictionLabel.BENIGN
            return ThreatPrediction(label = label, confidence = p.toFloat())
        }

        private fun tokenize(text: String): Set<String> =
            TOKEN_REGEX.findAll(text.lowercase()).map { it.value }.toHashSet()

        private fun sigmoid(z: Double): Double = 1.0 / (1.0 + exp(-z.coerceIn(-30.0, 30.0)))
    }
}
