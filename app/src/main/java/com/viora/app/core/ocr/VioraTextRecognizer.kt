package com.viora.app.core.ocr

import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.viora.app.domain.model.OcrResult

/**
 * Single on-device OCR engine (ML Kit Latin text recognition) shared by every
 * consumer — the live camera analyzer and the Share Sheet image processor both
 * delegate here, so recognition + result mapping exist in exactly one place.
 *
 * Emits plain [OcrResult]; no risk interpretation happens in this layer.
 */
class VioraTextRecognizer {

    private val client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Runs recognition asynchronously; callbacks arrive on the main thread.
     * Returns the underlying ML Kit [Task] so callers can observe completion
     * (e.g. for frame lifecycle management).
     */
    fun recognize(
        image: InputImage,
        onSuccess: (OcrResult) -> Unit,
        onFailure: ((Exception) -> Unit)? = null
    ): Task<Text> {
        return client.process(image)
            .addOnSuccessListener { visionText -> onSuccess(visionText.toOcrResult()) }
            .addOnFailureListener { e -> onFailure?.invoke(e) }
    }

    /** Releases the underlying ML Kit client. Call when no more input will arrive. */
    fun close() {
        client.close()
    }

    private fun Text.toOcrResult(): OcrResult {
        val lines = textBlocks
            .flatMap { it.lines }
            .map { it.text }
            .filter { it.isNotBlank() }
        return OcrResult(
            rawText = lines.joinToString("\n"),
            lineCount = lines.size,
            detectedAtMs = System.currentTimeMillis()
        )
    }
}
