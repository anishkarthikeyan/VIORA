package com.viora.app.core.camera

import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Single on-device barcode/QR decoding engine (ML Kit), shared by every
 * consumer — the live camera analyzer ([QrFrameAnalyzer]) and the Share Sheet
 * image processor both delegate here, mirroring how [com.viora.app.core.ocr.VioraTextRecognizer]
 * is already the one shared OCR engine — so QR decoding logic exists in
 * exactly one place instead of being duplicated per input source.
 */
class VioraBarcodeDecoder {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
    )

    /** Runs detection asynchronously; returns the underlying ML Kit [Task]. */
    fun process(image: InputImage): Task<List<Barcode>> = scanner.process(image)

    /** Releases the underlying ML Kit client. Call when no more input will arrive. */
    fun close() {
        scanner.close()
    }

    companion object {
        fun formatName(format: Int): String = when (format) {
            Barcode.FORMAT_QR_CODE -> "QR_CODE"
            Barcode.FORMAT_UPC_A -> "UPC_A"
            Barcode.FORMAT_UPC_E -> "UPC_E"
            Barcode.FORMAT_EAN_13 -> "EAN_13"
            Barcode.FORMAT_EAN_8 -> "EAN_8"
            Barcode.FORMAT_CODE_39 -> "CODE_39"
            Barcode.FORMAT_CODE_93 -> "CODE_93"
            Barcode.FORMAT_CODE_128 -> "CODE_128"
            Barcode.FORMAT_CODABAR -> "CODABAR"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_AZTEC -> "AZTEC"
            Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
            else -> "UNKNOWN($format)"
        }
    }
}
