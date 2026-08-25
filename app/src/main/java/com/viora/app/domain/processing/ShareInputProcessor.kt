package com.viora.app.domain.processing

import com.viora.app.domain.model.InputType
import com.viora.app.domain.model.VioraContext

/**
 * Result of processing shared content from the Android Share Sheet.
 */
sealed interface ShareProcessResult {

    /** Shareable content was understood and converted into a [VioraContext]. */
    data class Valid(val context: VioraContext) : ShareProcessResult

    /** Content was null, blank, or otherwise unusable — nothing to analyze. */
    data object Empty : ShareProcessResult
}

/**
 * Converts text received via `ACTION_SEND` into a [VioraContext].
 *
 * Extraction only — no safety judgement happens here; that is GuardianAI's job.
 *
 * Behaviour:
 * - URLs inside the text are detected and collected in [VioraContext.detectedUrls].
 *   Both explicit (`https://…`) and bare (`www.example.com`) forms are recognised.
 * - If the whole payload is a UPI URI, the existing UPI parser fills the payment
 *   fields (`upiId`, `amount`, …) instead of treating it as an opaque URL.
 * - Plain text without URLs becomes `InputType.TEXT` with the raw content preserved.
 * - Null / blank input yields [ShareProcessResult.Empty] so callers can degrade gracefully.
 */
class ShareInputProcessor(private val upiParser: com.viora.app.domain.parser.UpiParser? = null) {

    fun process(rawText: String?, sourceApplication: String? = null): ShareProcessResult {
        val text = rawText?.trim().orEmpty()
        if (text.isEmpty()) return ShareProcessResult.Empty

        // Whole payload is a UPI deep link → reuse the dedicated parser for payment fields.
        if (text.startsWith("upi://", ignoreCase = true)) {
            val parser = upiParser ?: return fallbackContext(text, sourceApplication)
            return when (val parsed = parser.parse(text)) {
                is com.viora.app.domain.parser.UpiParseResult.MalformedUpi -> ShareProcessResult.Empty
                is com.viora.app.domain.parser.UpiParseResult.UpiPayment ->
                    ShareProcessResult.Valid(parsed.context.copy(sourceApplication = sourceApplication))
                is com.viora.app.domain.parser.UpiParseResult.Url ->
                    ShareProcessResult.Valid(parsed.context.copy(sourceApplication = sourceApplication))
                is com.viora.app.domain.parser.UpiParseResult.Text ->
                    ShareProcessResult.Valid(parsed.context.copy(sourceApplication = sourceApplication))
            }
        }

        return fallbackContext(text, sourceApplication)
    }

    private fun fallbackContext(text: String, sourceApplication: String?): ShareProcessResult.Valid {
        val urls = extractUrls(text)

        val context = VioraContext(
            inputType = if (urls.isNotEmpty()) InputType.URL else InputType.TEXT,
            rawContent = text,
            extractedText = text,
            detectedUrls = urls,
            sourceApplication = sourceApplication
        )
        return ShareProcessResult.Valid(context)
    }

    companion object {
        /**
         * Matches:
         *  - explicit scheme URLs:  https://x, http://x  (stops at whitespace or closing bracket)
         *  - bare www. hosts:       www.example.com/path
         * Trailing sentence punctuation (. , ; : ! ? ) is trimmed off.
         */
        private val URL_PATTERN = Regex(
            pattern = "(?:https?://|www\\.)[^\\s<>\"']+",
            options = setOf(RegexOption.IGNORE_CASE)
        )

        /** Shared URL extraction used for both shared text and OCR'd screenshot text. */
        fun extractUrls(text: String): List<String> = URL_PATTERN.findAll(text)
            .map { it.value.trimEnd('.', ',', ';', ':', '!', '?', ')', ']') }
            .distinct()
            .toList()
    }
}
