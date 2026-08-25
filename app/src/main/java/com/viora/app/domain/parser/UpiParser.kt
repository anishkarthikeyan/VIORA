package com.viora.app.domain.parser

import com.viora.app.domain.model.InputType
import com.viora.app.domain.model.VioraContext
import java.net.URLDecoder

/**
 * Result of parsing scanned QR content.
 *
 * The parser ONLY extracts information. It never judges whether a payment is
 * safe or fraudulent — that is the job of ThreatEngine / GuardianAI.
 */
sealed interface UpiParseResult {
    /** Recognized UPI payment URI. Individual fields are null when absent from the payload. */
    data class UpiPayment(val context: VioraContext) : UpiParseResult

    /** Ordinary http(s) URL or www shorthand. */
    data class Url(val context: VioraContext) : UpiParseResult

    /** Plain text that is neither UPI nor a URL. */
    data class Text(val context: VioraContext) : UpiParseResult

    /** Looked like a UPI URI but could not be decoded. */
    data class MalformedUpi(val rawContent: String, val reason: String) : UpiParseResult
}

/**
 * Dedicated parser for UPI payment URIs such as:
 *   upi://pay?pa=merchant@upi&pn=Merchant&am=500&cu=INR&tn=Payment
 *
 * Implemented with pure Kotlin/JVM APIs (no android.net.Uri) so it can be unit
 * tested without Robolectric.
 */
class UpiParser {

    fun parse(rawContent: String): UpiParseResult {
        val trimmed = rawContent.trim()
        if (trimmed.isEmpty()) return UpiParseResult.Text(textContext(trimmed))

        val lowered = trimmed.lowercase()
        return when {
            lowered.startsWith(UPI_SCHEME) -> parseUpi(trimmed)
            URL_REGEX.matches(trimmed) || lowered.startsWith(WWW_PREFIX) ->
                UpiParseResult.Url(
                    VioraContext(
                        inputType = InputType.QR,
                        rawContent = trimmed,
                        detectedUrls = listOf(trimmed)
                    )
                )
            else -> UpiParseResult.Text(textContext(trimmed))
        }
    }

    private fun parseUpi(raw: String): UpiParseResult {
        val params = mutableMapOf<String, String>()

        val queryStart = raw.indexOf('?')
        if (queryStart >= 0) {
            val query = raw.substring(queryStart + 1)
            for (pair in query.split('&')) {
                if (pair.isEmpty()) continue

                val eq = pair.indexOf('=')
                val key = if (eq >= 0) pair.substring(0, eq) else pair
                val encodedValue = if (eq >= 0) pair.substring(eq + 1) else ""

                val decodedValue = try {
                    decode(encodedValue)
                } catch (e: IllegalArgumentException) {
                    return UpiParseResult.MalformedUpi(
                        rawContent = raw,
                        reason = "Invalid URL encoding in parameter '$key'"
                    )
                }
                params[key.trim().lowercase()] = decodedValue
            }
        }

        // Missing amount / merchant / currency / note are all acceptable:
        // every field in VioraContext except the identifiers is optional.
        return UpiParseResult.UpiPayment(
            VioraContext(
                inputType = InputType.QR,
                rawContent = raw,
                upiId = params[KEY_UPI_ID],
                merchantName = params[KEY_MERCHANT_NAME],
                amount = params[KEY_AMOUNT],
                currency = params[KEY_CURRENCY],
                note = params[KEY_NOTE]
            )
        )
    }

    private fun textContext(raw: String) = VioraContext(
        inputType = InputType.QR,
        rawContent = raw
    )

    /**
     * Percent-decoding per application/x-www-form-urlencoded rules.
     * '+' decodes to a space, which matches how UPI apps encode merchant names.
     */
    private fun decode(value: String): String =
        URLDecoder.decode(value.replace(ENCODED_PLUS, "%20"), Charsets.UTF_8)

    companion object {
        private const val UPI_SCHEME = "upi://"
        private const val WWW_PREFIX = "www."
        private val URL_REGEX = Regex("^(?i)https?://\\S+$")
        private val ENCODED_PLUS = Regex("\\+")

        private const val KEY_UPI_ID = "pa"
        private const val KEY_MERCHANT_NAME = "pn"
        private const val KEY_AMOUNT = "am"
        private const val KEY_CURRENCY = "cu"
        private const val KEY_NOTE = "tn"
    }
}
