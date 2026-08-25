package com.viora.app.domain.model

/**
 * Text extracted from a single camera scene via OCR.
 * Pure data: no risk interpretation happens here.
 */
data class OcrResult(
    /** Full recognized text, lines joined with newlines. Empty when nothing readable. */
    val rawText: String,
    /** Number of text lines recognized. */
    val lineCount: Int,
    /** Wall-clock timestamp (System.currentTimeMillis) of the capture. */
    val detectedAtMs: Long
) {
    val isEmpty: Boolean get() = rawText.isBlank()
}
