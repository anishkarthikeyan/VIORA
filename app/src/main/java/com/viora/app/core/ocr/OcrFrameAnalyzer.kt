package com.viora.app.core.ocr

import android.os.SystemClock
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.viora.app.core.camera.ChildFrameAnalyzer
import com.viora.app.domain.model.OcrResult

/**
 * Dedicated frame analyzer for live-camera OCR.
 *
 * Design notes:
 * - Throttling: OCR is heavier than barcode detection, so frames are only sent to
 *   the recognizer at most once per [SCAN_INTERVAL_MS]; everything in between is skipped.
 * - Smoothness: runs under STRATEGY_KEEP_ONLY_LATEST (set by the camera pipeline),
 *   so queued frames are dropped while recognition is busy — the preview never blocks.
 * - Frame ownership: implements [ChildFrameAnalyzer]; the composite pipeline closes
 *   the ImageProxy when all analyzers are done.
 * - Reuse: all ML Kit mechanics live in [VioraTextRecognizer]; this class only owns
 *   camera-frame conversion and throttling. The Share Sheet image processor reuses
 *   the same recognizer, so OCR logic is never duplicated.
 */
class OcrFrameAnalyzer(
    private val onOcrResult: (OcrResult) -> Unit,
    private val recognizer: VioraTextRecognizer = VioraTextRecognizer()
) : ChildFrameAnalyzer {

    private var lastScanStartMs = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy, onDone: () -> Unit) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            onDone()
            return
        }

        val nowMs = SystemClock.elapsedRealtime()

        // Requirement: do not run OCR on every frame.
        if (nowMs - lastScanStartMs < SCAN_INTERVAL_MS) {
            onDone()
            return
        }
        lastScanStartMs = nowMs

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        recognizer.recognize(
            image = inputImage,
            onSuccess = { result -> onOcrResult(result) },
            onFailure = { /* unreadable frame: nothing to emit */ }
        ).addOnCompleteListener {
            // Frame lifecycle is owned by CompositeFrameAnalyzer regardless of outcome.
            onDone()
        }
    }

    /** Releases the underlying ML Kit recognizer. Call when scanning stops. */
    override fun close() {
        recognizer.close()
    }

    companion object {
        /** Minimum gap between OCR passes (~1 pass every 2 seconds). */
        private const val SCAN_INTERVAL_MS = 2_000L
    }
}
