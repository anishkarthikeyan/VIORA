package com.viora.app.domain.fusion

import com.viora.app.domain.model.InputType
import com.viora.app.domain.model.OcrResult
import com.viora.app.domain.model.VioraContext

/**
 * Combines contributions from multiple sources into one [VioraContext].
 *
 * Core rules:
 * - Sources NEVER overwrite each other: QR-derived facts (upiId, merchantName,
 *   amount) and OCR-derived facts (visibleMerchant, displayedAmount) live in
 *   dedicated fields. Discrepancies between what is displayed on screen and what
 *   a payment payload claims are therefore PRESERVED for later analysis.
 * - Extensible: new sources (URL, message, image, AI) become new [FusionInput]
 *   variants plus one merge branch each.
 * - No security decisions: this is pure information merging.
 */
class ContextFusionEngine {

    fun fuse(inputs: List<FusionInput>): VioraContext {
        var fused = VioraContext(inputType = InputType.CAMERA, rawContent = "")

        inputs.forEach { input ->
            fused = when (input) {
                is FusionInput.Parsed -> mergeParsed(fused, input.context)
                is FusionInput.SceneText -> mergeSceneText(fused, input.result)
            }
        }
        return fused
    }

    /** Merges a structured context (parsed QR/UPI). Fills only still-empty fields. */
    private fun mergeParsed(fused: VioraContext, parsed: VioraContext): VioraContext = fused.copy(
        rawContent = fused.rawContent.ifEmpty { parsed.rawContent },
        upiId = fused.upiId ?: parsed.upiId,
        merchantName = fused.merchantName ?: parsed.merchantName,
        amount = fused.amount ?: parsed.amount,
        currency = fused.currency ?: parsed.currency,
        note = fused.note ?: parsed.note,
        extractedText = fused.extractedText ?: parsed.extractedText,
        detectedUrls = (fused.detectedUrls + parsed.detectedUrls).distinct(),
        sourceApplication = fused.sourceApplication ?: parsed.sourceApplication
    )

    /** Merges scene text (OCR): extracts visible merchant and displayed amount. */
    private fun mergeSceneText(fused: VioraContext, ocr: OcrResult): VioraContext {
        if (ocr.isEmpty) return fused

        val lines = ocr.rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val merchant = visibleMerchantFrom(lines)
        val amount = displayedAmountFrom(lines)
        val upiId = visibleUpiIdFrom(lines)

        return fused.copy(
            extractedText = fused.extractedText ?: ocr.rawText,
            // OCR-only fields; never collide with QR-derived values.
            visibleMerchant = fused.visibleMerchant ?: merchant,
            displayedAmount = fused.displayedAmount ?: amount,
            visibleUpiId = fused.visibleUpiId ?: upiId
        )
    }

    /**
     * First plausible merchant line: contains letters, is not a pure amount and
     * does not match generic payment signage ("SCAN TO PAY", "PAY", "UPI", ...).
     */
    private fun visibleMerchantFrom(lines: List<String>): String? =
        lines.firstOrNull { line ->
            line.any { it.isLetter() } &&
                !line.contains(CURRENCY_SYMBOL_REGEX) &&
                PAYMENT_SIGNAGE_KEYWORDS.none { line.lowercase().contains(it) }
        }?.takeIf { it.length >= MIN_MERCHANT_LINE_LENGTH }

    /** Amount rendered in the scene: "₹850", "Rs. 850", "INR 850", "Pay 850". */
    private fun displayedAmountFrom(lines: List<String>): String? {
        for (line in lines) {
            CURRENCY_AMOUNT_REGEX.find(line)?.let { return normalizeAmount(it.groupValues[1]) }
        }
        for (line in lines) {
            PAY_PREFIX_AMOUNT_REGEX.find(line)?.let { return normalizeAmount(it.groupValues[1]) }
        }
        return null
    }

    private fun normalizeAmount(raw: String): String = raw.replace(",", "")

    /**
     * First UPI-ID-shaped token in the scene, e.g. "nied.foundation@ybl". A UPI
     * VPA is syntactically identical to an email address (handle@psp) — this is
     * a best-effort heuristic, same conservative spirit as [visibleMerchantFrom]/
     * [displayedAmountFrom]; an unrelated visible email would be misread as a
     * UPI ID. There is no reliable way to distinguish the two from text alone.
     */
    private fun visibleUpiIdFrom(lines: List<String>): String? =
        lines.firstNotNullOfOrNull { UPI_ID_REGEX.find(it)?.value }

    companion object {
        private val CURRENCY_AMOUNT_REGEX =
            Regex("(?:₹|rs\\.?|inr)\\s*([\\d,]+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)

        private val PAY_PREFIX_AMOUNT_REGEX =
            Regex("\\bpay(?:ing)?\\s*(?:₹|rs\\.?|inr)?\\s*([\\d,]+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)

        private val CURRENCY_SYMBOL_REGEX = Regex("[₹]|(?i)\\b(?:rs|inr)\\b")

        private val PAYMENT_SIGNAGE_KEYWORDS = setOf(
            "scan to pay", "scan & pay", "pay now", "upi", "qr code", "google pay", "phonepe", "paytm"
        )

        private const val MIN_MERCHANT_LINE_LENGTH = 3

        private val UPI_ID_REGEX = Regex("[a-zA-Z0-9.\\-_]{2,}@[a-zA-Z]{2,}")
    }
}
