package com.viora.app.core.ocr

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.viora.app.core.camera.VioraBarcodeDecoder
import com.viora.app.domain.model.OcrResult
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** Result of processing a shared/gallery image: OCR is required; a QR/barcode payload is optional. */
data class SharedImageResult(
    val ocr: OcrResult,
    /** Raw content of the first detected barcode/QR in the image, or null if none was found. */
    val qrRawContent: String?
)

/**
 * Share-Sheet / gallery image processor: shared image Uri → decoded bitmap →
 * [OcrResult] + optional QR/barcode payload.
 *
 * Reuses [VioraTextRecognizer] and [VioraBarcodeDecoder] — no ML Kit logic is
 * duplicated; this class only owns Uri decoding/downscaling and suspending
 * until both recognizers complete. QR detection runs on the SAME decoded
 * bitmap OCR already used, so a screenshot containing both a UPI QR and
 * visible payment text yields both — mirroring what the live camera pipeline
 * already does with two ChildFrameAnalyzers on one frame.
 *
 * No risk interpretation: the caller feeds the result into UpiParser/
 * ContextFusionEngine.
 */
class SharedImageProcessor(
    private val recognizer: VioraTextRecognizer = VioraTextRecognizer(),
    private val barcodeDecoder: VioraBarcodeDecoder = VioraBarcodeDecoder()
) {

    /**
     * Decodes the image, then runs OCR and QR detection on it. Returns null only
     * when the image itself cannot be decoded/recognized (missing/corrupt
     * stream, or OCR recognition failure) — callers degrade gracefully. QR
     * detection is always best-effort: absence or failure to decode a barcode
     * never fails the whole result, it just means [SharedImageResult.qrRawContent]
     * is null.
     */
    suspend fun process(uri: Uri, resolver: ContentResolver): SharedImageResult? {
        val bitmap = decodeDownsampled(resolver, uri) ?: return null
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        val ocr = recognizeText(inputImage) ?: return null
        val qrRawContent = detectQr(inputImage)

        return SharedImageResult(ocr = ocr, qrRawContent = qrRawContent)
    }

    /** Releases the underlying ML Kit clients when no more images will arrive. */
    fun close() {
        recognizer.close()
        barcodeDecoder.close()
    }

    private suspend fun recognizeText(image: InputImage): OcrResult? =
        suspendCancellableCoroutine { continuation ->
            recognizer.recognize(
                image = image,
                onSuccess = { result -> continuation.resume(result) },
                onFailure = { continuation.resume(null) }
            )
        }

    /** Best-effort: no barcode, an unreadable one, or a detector failure all yield null. */
    private suspend fun detectQr(image: InputImage): String? = try {
        suspendCancellableCoroutine { continuation ->
            barcodeDecoder.process(image)
                .addOnSuccessListener { barcodes ->
                    val rawContent = barcodes.firstNotNullOfOrNull { it.rawValue?.takeIf(String::isNotEmpty) }
                    continuation.resume(rawContent)
                }
                .addOnFailureListener { continuation.resume(null) }
        }
    } catch (e: Exception) {
        null
    }

    /**
     * Two-pass decode with inSampleSize so a 12 MP screenshot does not blow up
     * memory; ML Kit works fine on ~[MAX_DIMENSION] px inputs.
     */
    private fun decodeDownsampled(resolver: ContentResolver, uri: Uri): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            null // not decodable as an image
        } else {
            var sampleSize = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_DIMENSION) {
                sampleSize *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }
    } catch (_: Exception) {
        null // security exception from a revoked share grant, corrupt data, OOM, ...
    }

    companion object {
        /**
         * 4096 px cap: ML Kit needs text ~16-20 px tall; aggressive downsampling
         * of dense screenshots (bank SMS, payment apps) destroys small print.
         * Also comfortably large enough for reliable QR detection.
         */
        private const val MAX_DIMENSION = 4096
    }
}
