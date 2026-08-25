package com.viora.app.core.camera

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

/**
 * Thin, framework-level wrapper around CameraX.
 *
 * All camera mechanics live here — outside of Compose — so the UI layer only
 * renders a [PreviewView] and delegates start/stop to this controller.
 *
 * Use cases: rear-camera Preview + optional ImageAnalysis (QR detection).
 */
class VioraCameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {

    private val mainExecutor = ContextCompat.getMainExecutor(context)

    private var cameraProvider: ProcessCameraProvider? = null
    private var previewView: PreviewView? = null
    private var isStarted = false

    /**
     * Asynchronously obtains the provider, then binds the use cases.
     * [imageAnalyzer] is optional; when supplied it receives camera frames.
     */
    fun start(
        previewView: PreviewView,
        imageAnalyzer: ImageAnalysis.Analyzer? = null,
        onError: (Throwable) -> Unit = {}
    ) {
        this.previewView = previewView
        if (isStarted) return
        isStarted = true

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                if (!isStarted) {
                    // stop() was called while the provider was still loading.
                    runCatching { future.get().unbindAll() }
                    return@addListener
                }
                try {
                    cameraProvider = future.get()
                    bindUseCases(imageAnalyzer)
                } catch (t: Throwable) {
                    onError(t)
                }
            },
            mainExecutor
        )
    }

    private fun bindUseCases(imageAnalyzer: ImageAnalysis.Analyzer?) {
        val provider = cameraProvider ?: return
        val view = previewView ?: return

        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(view.surfaceProvider) }

        // STRATEGY_KEEP_ONLY_LATEST drops frames while analysis runs -> smooth preview.
        // ResolutionSelector targets ~1080p: the CameraX default of 640x480 starves
        // ML Kit OCR (small text needs ~16-20 px height to be recognized reliably).
        val analysis = imageAnalyzer?.let { analyzer ->
            ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1920, 1080),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                            )
                        )
                        .build()
                )
                .build()
                .also { it.setAnalyzer(mainExecutor, analyzer) }
        }

        provider.unbindAll()
        // Lifecycle-aware binding: CameraX pauses/resumes with the owner's lifecycle.
        val useCases = buildList {
            add(preview)
            analysis?.let { add(it) }
        }
        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, *useCases.toTypedArray())
    }

    /**
     * Unbinds all use cases so the camera is released for other apps.
     * Safe to call multiple times and before the provider has loaded.
     */
    fun stop() {
        isStarted = false
        previewView = null
        mainExecutor.execute {
            runCatching { cameraProvider?.unbindAll() }
        }
    }
}
