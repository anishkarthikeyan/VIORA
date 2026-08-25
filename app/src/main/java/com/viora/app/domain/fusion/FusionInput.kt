package com.viora.app.domain.fusion

import com.viora.app.domain.model.OcrResult
import com.viora.app.domain.model.VioraContext

/**
 * A single contribution to the fused [VioraContext].
 *
 * The sealed design makes adding future sources (URL, message, image, AI
 * reasoning) a matter of declaring a new variant and teaching the fusion engine
 * how to merge it — existing sources remain untouched.
 */
sealed interface FusionInput {

    /** Structured payload already normalized into a [VioraContext] (parsed QR/UPI). */
    data class Parsed(val context: VioraContext) : FusionInput

    /** Raw visible text captured from the camera scene (OCR). */
    data class SceneText(val result: OcrResult) : FusionInput

    // Future variants, e.g.:
    // data class UrlInspection(...) : FusionInput
    // data class MessageContent(...) : FusionInput
    // data class ImageMetadata(...) : FusionInput
    // data class AiReasoning(...) : FusionInput
}
