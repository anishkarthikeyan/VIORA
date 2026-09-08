package com.viora.app.core.camera

import android.os.SystemClock
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage

/**
 * Dedicated frame analyzer for QR / barcode detection.
 *
 * Design notes:
 * - Generic detection: scans ALL barcode formats (not just UPI QRs) and reports the type.
 * - Throttling: skips frames that arrive faster than [SCAN_INTERVAL_MS] so we do not
 *   process every frame unnecessarily.
 * - Deduplication: suppresses re-emitting the same content while it is still in view,
 *   unless the duplicate cooldown has elapsed.
 * - Smoothness: CameraX runs this with STRATEGY_KEEP_ONLY_LATEST, so frames are dropped
 *   (never queued) when ML Kit is busy; each analyzed frame is closed exactly once.
 */
class QrFrameAnalyzer(
    private val onQrDetected: (rawContent: String, formatName: String) -> Unit
) : ChildFrameAnalyzer {

    private val decoder = VioraBarcodeDecoder()

    private var lastEmittedContent: String? = null
    private var lastScanStartMs = 0L
    private var lastEmitMs = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy, onDone: () -> Unit) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            onDone()
            return
        }

        val nowMs = SystemClock.elapsedRealtime()

        // Requirement: do not process every frame unnecessarily.
        if (nowMs - lastScanStartMs < SCAN_INTERVAL_MS) {
            onDone()
            return
        }
        lastScanStartMs = nowMs

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        decoder.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val barcode = barcodes.firstOrNull() ?: return@addOnSuccessListener
                val rawContent = barcode.rawValue ?: return@addOnSuccessListener

                // Requirement: prevent processing/emitting the same QR repeatedly.
                if (rawContent == lastEmittedContent && nowMs - lastEmitMs < DUPLICATE_COOLDOWN_MS) {
                    return@addOnSuccessListener
                }
                lastEmitMs = nowMs
                lastEmittedContent = rawContent

                onQrDetected(rawContent, VioraBarcodeDecoder.formatName(barcode.format))
            }
            .addOnCompleteListener {
                // Frame lifecycle is owned by CompositeFrameAnalyzer.
                onDone()
            }
    }

    /** Releases the underlying ML Kit client. Call when scanning stops. */
    override fun close() {
        decoder.close()
    }

    companion object {
        /** Minimum gap between frames sent to ML Kit. */
        private const val SCAN_INTERVAL_MS = 250L

        /** How long a repeated identical code stays suppressed. */
        private const val DUPLICATE_COOLDOWN_MS = 2_000L
    }
}
