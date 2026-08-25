package com.viora.app.core.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.util.concurrent.atomic.AtomicInteger

/**
 * A frame consumer that participates in the shared CameraX [ImageAnalysis] pipeline.
 *
 * Ownership contract: implementations analyze the given [ImageProxy] but MUST NOT
 * close it — the [CompositeFrameAnalyzer] closes the frame once every child has
 * reported completion via the [onDone] callback.
 */
interface ChildFrameAnalyzer {
    fun analyze(imageProxy: ImageProxy, onDone: () -> Unit)

    /** Releases underlying resources (e.g. ML Kit clients). */
    fun close()
}

/**
 * Fan-out analyzer for the single ImageAnalysis use case.
 *
 * Forwards each camera frame to all children (QR detection, OCR, ...) in parallel
 * and recycles the frame only when every child is finished. Children are expected
 * to throttle themselves so expensive work (ML Kit) does not run on every frame.
 */
class CompositeFrameAnalyzer(
    private val children: List<ChildFrameAnalyzer>
) : ImageAnalysis.Analyzer {

    private val pendingChildren = AtomicInteger()

    override fun analyze(imageProxy: ImageProxy) {
        if (children.isEmpty()) {
            imageProxy.close()
            return
        }
        pendingChildren.set(children.size)
        children.forEach { child ->
            child.analyze(imageProxy) { onChildFinished(imageProxy) }
        }
    }

    private fun onChildFinished(imageProxy: ImageProxy) {
        if (pendingChildren.decrementAndGet() == 0) {
            imageProxy.close()
        }
    }
}
