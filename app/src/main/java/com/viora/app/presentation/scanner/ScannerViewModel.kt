package com.viora.app.presentation.scanner

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viora.app.core.ocr.SharedImageProcessor
import com.viora.app.ai.GuardianAI
import com.viora.app.ai.ThreatEngine
import com.viora.app.domain.model.InputType
import com.viora.app.domain.model.VioraContext
import com.viora.app.domain.fusion.ContextFusionEngine
import com.viora.app.domain.fusion.FusionInput
import com.viora.app.domain.model.OcrResult
import com.viora.app.domain.parser.UpiParseResult
import com.viora.app.domain.parser.UpiParser
import com.viora.app.domain.processing.ShareInputProcessor
import com.viora.app.domain.processing.ShareProcessResult
import com.viora.app.domain.threat.ThreatAssessment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class QrDetection(
    val rawContent: String,
    val formatName: String,
    /** Parser verdict label for the dev banner, e.g. "UPI_PAYMENT", "URL", "TEXT". */
    val parseKind: String,
    /** Populated when the parser produced a VioraContext; null only for malformed input. */
    val parsedContext: VioraContext?
)

data class SharedAnalysis(
    val rawText: String,
    val detectedUrls: List<String>,
    val sourceApplication: String?,
    val parsedContext: VioraContext?
)

data class ScannerUiState(
    val assessment: ThreatAssessment? = null,
    val isLoading: Boolean = false,
    /** Latest detection metadata for the development banner. */
    val qrDetection: QrDetection? = null,
    /** Latest OCR text for the development debug panel. No risk interpretation yet. */
    val ocrResult: OcrResult? = null,
    /** Content received via the Android Share Sheet. */
    val sharedAnalysis: SharedAnalysis? = null,
    /** The VioraContext behind the current assessment — set by both QR and share flows. */
    val resultContext: VioraContext? = null,
    /** Whether the risk overlay should currently be shown (dismissable by the user). */
    val showOverlay: Boolean = false
)

/**
 * Full pipeline: camera frame -> QR detection -> parsing -> GuardianAI analysis.
 * The risk overlay reacts to real payloads instead of mock levels.
 */
class ScannerViewModel(
    private val guardianAI: GuardianAI = ThreatEngine(),
    private val upiParser: UpiParser = UpiParser(),
    private val fusionEngine: ContextFusionEngine = ContextFusionEngine(),
    private val shareProcessor: ShareInputProcessor = ShareInputProcessor(upiParser),
    private val sharedImageProcessor: SharedImageProcessor = SharedImageProcessor()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    // Latest contributions per source; re-fused whenever any of them updates.
    private var lastQrContext: VioraContext? = null
    private var lastOcrResult: OcrResult? = null

    /** Called by the camera pipeline whenever a new QR/barcode is detected. */
    fun onQrDetected(rawContent: String, formatName: String) {
        // Extraction only — safety evaluation happens in GuardianAI below.
        val parseResult = upiParser.parse(rawContent)
        Log.d(TAG, "QR detected [$formatName]: $rawContent -> $parseResult")

        val parsedContext: VioraContext? = when (parseResult) {
            is UpiParseResult.UpiPayment -> parseResult.context
            is UpiParseResult.Url -> parseResult.context
            is UpiParseResult.Text -> parseResult.context
            is UpiParseResult.MalformedUpi -> null
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                qrDetection = QrDetection(
                    rawContent = rawContent,
                    formatName = formatName,
                    parseKind = when (parseResult) {
                        is UpiParseResult.UpiPayment -> "UPI_PAYMENT"
                        is UpiParseResult.Url -> "URL"
                        is UpiParseResult.Text -> "TEXT"
                        is UpiParseResult.MalformedUpi -> "MALFORMED_UPI"
                    },
                    parsedContext = parsedContext
                )
            )
        }

        if (parsedContext == null) {
            // Malformed input: nothing to analyze; keep the previous assessment visible.
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        lastQrContext = parsedContext
        refreshAnalysis()
    }

    /** Called by the camera pipeline with throttled OCR output. Extraction only. */
    fun onOcrResult(result: OcrResult) {
        _uiState.update { it.copy(ocrResult = result) }
        Log.d(TAG, "OCR [${result.lineCount} lines]: ${result.rawText.take(120)}")

        lastOcrResult = result
        refreshAnalysis()
    }

    /** Fuses all available sources, then runs GuardianAI on the fused context. */
    private fun refreshAnalysis() {
        val inputs = buildList {
            lastQrContext?.let { add(FusionInput.Parsed(it)) }
            lastOcrResult?.takeIf { !it.isEmpty }?.let { add(FusionInput.SceneText(it)) }
        }
        if (inputs.isEmpty()) return

        val fusedContext = fusionEngine.fuse(inputs)

        _uiState.update { it.copy(isLoading = true, resultContext = fusedContext) }
        viewModelScope.launch {
            val assessment = guardianAI.analyze(fusedContext, ThreatAssessment.neutral())
            // A fresh assessment re-shows the overlay, even if it was dismissed before.
            _uiState.update {
                it.copy(assessment = assessment, isLoading = false, showOverlay = true)
            }
        }
    }

    /** User dismissed the overlay; the camera stays live and analysis continues. */
    fun dismissOverlay() {
        _uiState.update { it.copy(showOverlay = false) }
    }

    /**
     * Share-intent pipeline: ACTION_SEND text → input processor → VioraContext →
     * GuardianAI → Result. Independent of the camera pipeline; empty or malformed
     * share content is ignored gracefully.
     */
    fun onSharedText(rawText: String?, sourceApplication: String? = null): Boolean {
        return when (val result = shareProcessor.process(rawText, sourceApplication)) {
            is ShareProcessResult.Empty -> {
                Log.w(TAG, "Share received but content was empty/unusable — ignoring")
                false
            }
            is ShareProcessResult.Valid -> {
                val context = result.context
                Log.d(TAG, "Share processed [${context.inputType}]: urls=${context.detectedUrls}")
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        sharedAnalysis = SharedAnalysis(
                            rawText = context.rawContent,
                            detectedUrls = context.detectedUrls,
                            sourceApplication = context.sourceApplication,
                            parsedContext = context
                        ),
                        resultContext = context
                    )
                }
                viewModelScope.launch {
                    val assessment = guardianAI.analyze(context, ThreatAssessment.neutral())
                    _uiState.update {
                        it.copy(assessment = assessment, isLoading = false, showOverlay = true)
                    }
                }
                true
            }
        }
    }

    /**
     * Shared-image pipeline (screenshots, payment screenshots, fake bank/reward
     * messages): ACTION_SEND image Uri → SharedImageProcessor → ML Kit OCR →
     * extracted text → ContextFusionEngine → VioraContext → GuardianAI → Result.
     * Reuses the camera OCR engine and fusion layer — no duplicated logic.
     * Unreadable images or images without text are handled gracefully.
     */
    suspend fun onSharedImage(uri: Uri, resolver: ContentResolver): Boolean {
        _uiState.update { it.copy(isLoading = true) }

        val ocr = withContext(Dispatchers.IO) { sharedImageProcessor.process(uri, resolver) }

        if (ocr == null) {
            Log.w(TAG, "Shared image could not be decoded — ignoring")
            _uiState.update { it.copy(isLoading = false) }
            return false
        }
        if (ocr.isEmpty) {
            Log.w(TAG, "Shared image contained no readable text — ignoring")
            _uiState.update { it.copy(isLoading = false) }
            return false
        }

        Log.d(TAG, "Shared-image OCR [${ocr.lineCount} lines]: ${ocr.rawText.take(120)}")

        // Fuse through the same engine as the camera path so merchant/amount
        // extraction heuristics apply identically to screenshot content.
        val fusedContext = fusionEngine.fuse(listOf(FusionInput.SceneText(ocr)))
            .copy(inputType = InputType.IMAGE, rawContent = ocr.rawText)

        // Also surface URLs that appear inside the screenshot text (shared extractor).
        val contextWithUrls = fusedContext.copy(
            detectedUrls = (
                fusedContext.detectedUrls + ShareInputProcessor.extractUrls(ocr.rawText)
                ).distinct()
        )

        _uiState.update {
            it.copy(
                sharedAnalysis = SharedAnalysis(
                    rawText = ocr.rawText,
                    detectedUrls = contextWithUrls.detectedUrls,
                    sourceApplication = null,
                    parsedContext = contextWithUrls
                ),
                resultContext = contextWithUrls
            )
        }

        val assessment = guardianAI.analyze(contextWithUrls, ThreatAssessment.neutral())
        _uiState.update {
            it.copy(assessment = assessment, isLoading = false, showOverlay = true)
        }
        return true
    }

    override fun onCleared() {
        sharedImageProcessor.close()
        super.onCleared()
    }

    companion object {
        private const val TAG = "VioraScanner"
    }
}
