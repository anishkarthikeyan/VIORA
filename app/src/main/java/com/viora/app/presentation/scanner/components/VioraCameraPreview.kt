package com.viora.app.presentation.scanner.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.viora.app.core.camera.CompositeFrameAnalyzer
import com.viora.app.core.camera.QrFrameAnalyzer
import com.viora.app.core.ocr.OcrFrameAnalyzer
import com.viora.app.domain.model.OcrResult
import com.viora.app.core.camera.VioraCameraController

/**
 * Compose wrapper that renders the live CameraX preview with QR detection + OCR.
 * All camera mechanics are delegated to [VioraCameraController]; frame fan-out to
 * both analyzers is handled by [CompositeFrameAnalyzer]. This composable only owns
 * view/controller lifecycles and teardown.
 */
@Composable
fun VioraCameraPreview(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onQrDetected: (rawContent: String, formatName: String) -> Unit = { _, _ -> },
    onOcrResult: (OcrResult) -> Unit = {},
    onError: (Throwable) -> Unit = {}
) {
    val context = LocalContext.current

    // Stable references so analyzers always invoke the latest callbacks.
    val currentOnQrDetected by rememberUpdatedState(onQrDetected)
    val currentOnOcrResult by rememberUpdatedState(onOcrResult)
    val currentOnError by rememberUpdatedState(onError)

    // One controller + one analyzer set per composition instance.
    val controller = remember(lifecycleOwner) {
        VioraCameraController(context, lifecycleOwner)
    }
    val qrAnalyzer = remember(lifecycleOwner) {
        QrFrameAnalyzer { raw, format -> currentOnQrDetected(raw, format) }
    }
    val ocrAnalyzer = remember(lifecycleOwner) {
        OcrFrameAnalyzer(onOcrResult = { result -> currentOnOcrResult(result) })
    }
    val compositeAnalyzer = remember(lifecycleOwner) {
        CompositeFrameAnalyzer(listOf(qrAnalyzer, ocrAnalyzer))
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            controller.stop()
            qrAnalyzer.close()
            ocrAnalyzer.close()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                controller.start(
                    previewView = this,
                    imageAnalyzer = compositeAnalyzer,
                    onError = { currentOnError(it) }
                )
            }
        }
    )
}
