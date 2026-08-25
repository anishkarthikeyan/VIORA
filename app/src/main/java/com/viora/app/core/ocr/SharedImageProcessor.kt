package com.viora.app.core.ocr

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.viora.app.domain.model.OcrResult
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Share-Sheet image processor: shared image Uri → decoded bitmap → ML Kit OCR →
 * [OcrResult].
 *
 * Reuses [VioraTextRecognizer] — no ML Kit logic is duplicated; this class only
 * owns Uri decoding/downscaling and suspending until recognition completes.
 *
 * No risk interpretation: the caller feeds the [OcrResult] into ContextFusionEngine.
 */
class SharedImageProcessor(
    private val recognizer: VioraTextRecognizer = VioraTextRecognizer()
) {

    /**
     * Decodes and recognizes text in the shared image. Suspending so callers can
     * run it off the main thread. Returns null when the image cannot be decoded
     * (missing/corrupt stream) — callers degrade gracefully.
     */
    suspend fun process(uri: Uri, resolver: ContentResolver): OcrResult? {
        val bitmap = decodeDownsampled(resolver, uri) ?: return null
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        return suspendCancellableCoroutine { continuation ->
            recognizer.recognize(
                image = inputImage,
                onSuccess = { result -> continuation.resume(result) },
                onFailure = { continuation.resume(null) }
            )
        }
    }

    /** Releases the underlying ML Kit recognizer when no more images will arrive. */
    fun close() {
        recognizer.close()
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
         */
        private const val MAX_DIMENSION = 4096
    }
}
