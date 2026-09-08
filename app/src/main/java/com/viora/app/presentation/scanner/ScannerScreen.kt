package com.viora.app.presentation.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viora.app.presentation.components.PrimaryActionButton
import com.viora.app.presentation.components.VioraDarkBackground
import com.viora.app.presentation.components.VioraDebugPanel
import com.viora.app.presentation.components.VioraPrimaryCyan
import com.viora.app.presentation.components.VioraRadius
import com.viora.app.presentation.components.VioraRiskOverlay
import com.viora.app.presentation.components.VioraSpacing
import com.viora.app.presentation.scanner.components.VioraCameraPreview

@Composable
fun ScannerScreen(
    onBackClick: () -> Unit,
    onViewResultClick: () -> Unit,
    viewModel: ScannerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    // Ask for the camera permission as soon as the scanner opens.
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = VioraDarkBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasCameraPermission) {
                // Live rear-camera preview, full-bleed behind the scanning UI.
                VioraCameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onQrDetected = viewModel::onQrDetected,
                    onOcrResult = viewModel::onOcrResult,
                    onError = { throwable ->
                        android.util.Log.e("VioraScanner", "Camera failed", throwable)
                    }
                )
            } else {
                CameraPermissionDenied(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }

            // Viewfinder reticle drawn over the live preview (or placeholder state)
            if (hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(240.dp)
                        .border(2.dp, VioraPrimaryCyan.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                )
            }

            // Top Header Overlay + development QR-detection state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = VioraSpacing.xl, end = VioraSpacing.xl)
            ) {
                Text(
                    text = "VIORA SCANNER",
                    color = VioraPrimaryCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Point at a QR code or payment link to check it",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )

                uiState.qrDetection?.let { detection ->
                    Spacer(modifier = Modifier.height(VioraSpacing.md))
                    QrDetectedDevBanner(detection = detection)
                }
            }

            // Real-time Warning Overlay, OCR Debug Panel & Action Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
            ) {
                // Development-only OCR output panel
                uiState.ocrResult?.takeIf { !it.isEmpty }?.let { ocr ->
                    OcrDebugPanel(ocrResult = ocr)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (uiState.isLoading) {
                    Text(
                        text = "ANALYZING…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Viora Real-time Warning Overlay — floats over the live camera,
                // never navigates away. Dismiss/details handled here.
                VioraRiskOverlay(
                    assessment = uiState.assessment,
                    visible = uiState.showOverlay && !uiState.isLoading && uiState.assessment != null,
                    context = uiState.qrDetection?.parsedContext,
                    onDetailsClick = onViewResultClick,
                    onDismissClick = viewModel::dismissOverlay
                )

                Spacer(modifier = Modifier.height(VioraSpacing.sm))

                PrimaryActionButton(
                    text = "View Detailed Analysis",
                    onClick = onViewResultClick,
                    modifier = Modifier.padding(horizontal = VioraSpacing.lg)
                )
            }
        }
    }
}

/** Development-only panel showing the latest throttled OCR extraction. */
@Composable
private fun OcrDebugPanel(ocrResult: com.viora.app.domain.model.OcrResult) {
    VioraDebugPanel {
        Text(
            text = "OCR DEBUG (${ocrResult.lineCount} lines)",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = ocrResult.rawText,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 4,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

/** Development-only state for the QR-detection milestone. */
@Composable
private fun QrDetectedDevBanner(detection: com.viora.app.presentation.scanner.QrDetection) {
    Surface(
        color = VioraPrimaryCyan.copy(alpha = 0.15f),
        shape = RoundedCornerShape(VioraRadius.sm),
        border = androidx.compose.foundation.BorderStroke(1.dp, VioraPrimaryCyan),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(VioraSpacing.md)) {
            Text(
                text = "QR DETECTED",
                color = VioraPrimaryCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Type: ${detection.formatName}  •  Parsed as: ${detection.parseKind}",
                color = Color.White,
                fontSize = 12.sp
            )
            detection.parsedContext?.let { ctx ->
                if (ctx.upiId != null) {
                    Text(
                        text = buildString {
                            append("UPI: ${ctx.upiId}")
                            ctx.merchantName?.let { append("  •  $it") }
                            if (ctx.amount != null) {
                                append("  •  ${ctx.currency ?: ""}${ctx.amount}")
                            }
                            ctx.note?.let { append("  •  $it") }
                        },
                        color = VioraPrimaryCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else if (ctx.detectedUrls.isNotEmpty()) {
                    Text(
                        text = "URL: ${ctx.detectedUrls.first()}",
                        color = VioraPrimaryCyan,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = "Content: ${detection.rawContent}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

/** Graceful state shown when the camera permission is missing or was denied. */
@Composable
private fun CameraPermissionDenied(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111622)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Camera access needed",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "VIORA uses the camera to inspect QR codes and payment links in real time before you trust them. No frames are stored or uploaded.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            PrimaryActionButton(text = "Grant camera access", onClick = onRequestPermission)
        }
    }
}
