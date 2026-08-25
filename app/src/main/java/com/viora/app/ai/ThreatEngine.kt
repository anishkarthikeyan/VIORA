package com.viora.app.ai

import com.viora.app.domain.model.VioraContext
import com.viora.app.domain.threat.RiskLevel
import com.viora.app.domain.threat.ThreatAssessment
import com.viora.app.domain.threat.ThreatSignal

/**
 * Deterministic threat engine, v1.
 *
 * Implements exactly three explainable signals derived from the fused
 * [VioraContext]:
 *
 *  1. AMOUNT_MISMATCH   — the amount shown on screen differs from the payment payload.
 *  2. MERCHANT_MISMATCH — the visible merchant name does not match the payload's pn.
 *  3. UNKNOWN_RECIPIENT — a payment is requested but the recipient identity cannot
 *     be verified. IMPORTANT: an unknown UPI ID is NOT fraud by itself; this signal
 *     only asks the user to verify, it never claims a scam.
 *
 * Conservative by design: every signal carries its own reason string so the UI can
 * show WHY each point was added. No AI, no network, fully reproducible.
 */
class ThreatEngine : GuardianAI {

    override suspend fun analyze(
        context: VioraContext,
        assessment: ThreatAssessment
    ): ThreatAssessment {
        val signals = mutableListOf<ThreatSignal>()

        checkAmountMismatch(context, signals)
        checkMerchantMismatch(context, signals)
        checkUnknownRecipient(context, signals)

        val totalScore = signals.sumOf { it.score }.coerceIn(0, 100)
        val level = levelFor(totalScore)

        return ThreatAssessment(
            score = if (signals.isEmpty()) SAFE_BASE_SCORE else totalScore,
            riskLevel = level,
            signals = signals,
            explanation = explanation(signals, level),
            recommendedAction = recommendedAction(level)
        )
    }

    // ------------------------------------------------------------- Signals

    /**
     * Screen shows one amount, payload requests another (e.g. menu ₹850 vs
     * upi://pay?...&am=5000). Strong tampering indicator.
     */
    private fun checkAmountMismatch(context: VioraContext, signals: MutableList<ThreatSignal>) {
        val displayed = context.displayedAmount?.let { parseAmount(it) }
        val requested = context.amount?.let { parseAmount(it) }

        if (displayed != null && requested != null && displayed != requested) {
            signals += ThreatSignal(
                id = "AMOUNT_MISMATCH",
                score = 35,
                severity = RiskLevel.SUSPICIOUS,
                description =
                    "Amount mismatch: screen shows ${context.displayedAmount} but the " +
                        "payment payload requests ${context.amount}."
            )
        }
    }

    /**
     * Visible business name does not correspond to the name embedded in the UPI
     * handle. Compared conservatively: only flagged when the two names share no
     * significant word at all.
     */
    private fun checkMerchantMismatch(context: VioraContext, signals: MutableList<ThreatSignal>) {
        val visible = context.visibleMerchant
        val claimed = context.merchantName

        if (visible != null && claimed != null && !namesRelate(visible, claimed)) {
            signals += ThreatSignal(
                id = "MERCHANT_MISMATCH",
                score = 25,
                severity = RiskLevel.SUSPICIOUS,
                description =
                    "Merchant name mismatch: scene shows \"$visible\" but the payment " +
                        "payload claims \"$claimed\"."
            )
        }
    }

    /**
     * Money is being requested but we cannot tell WHO receives it. Deliberately
     * low-scored: an unknown recipient is common in legitimate person-to-person
     * payments and must not be treated as fraud on its own.
     */
    private fun checkUnknownRecipient(context: VioraContext, signals: MutableList<ThreatSignal>) {
        val paymentRequested = context.amount != null || context.upiId != null
        val merchantKnown = !context.merchantName.isNullOrBlank()

        if (paymentRequested && !merchantKnown) {
            signals += ThreatSignal(
                id = "UNKNOWN_RECIPIENT",
                // Exactly one step above SAFE: enough to ask for verification,
                // never enough alone to reach SUSPICIOUS.
                score = 20,
                severity = RiskLevel.VERIFY,
                description =
                    "Recipient identity could not be verified${describeHandle(context)}. " +
                        "This alone does not indicate fraud — verify before paying."
            )
        }
    }

    // -------------------------------------------------------------- Helpers

    private fun describeHandle(context: VioraContext): String {
        val upiId = context.upiId ?: return "."
        return if (isOpaqueNumericHandle(upiId)) {
            " The UPI handle \"$upiId\" is numeric and gives no name to check."
        } else {
            " The UPI handle is \"$upiId\" but no merchant name accompanies it."
        }
    }

    private fun isOpaqueNumericHandle(upiId: String): Boolean {
        val handle = upiId.substringBefore('@')
        return handle.length >= MIN_OPAQUE_HANDLE_LENGTH && handle.all { it.isDigit() }
    }

    /** Parses "₹850", "1,250.50", "Rs 99" style values into a comparable number. */
    private fun parseAmount(raw: String): Double? =
        raw.replace(AMOUNT_NOISE_REGEX, "").toDoubleOrNull()

    /**
     * True when the two names plausibly refer to the same business:
     * containment either way or any shared significant word.
     */
    private fun namesRelate(visible: String, claimed: String): Boolean {
        val a = tokenize(visible)
        val b = tokenize(claimed)
        if (a.isEmpty() || b.isEmpty()) return true // nothing to compare -> be conservative

        return a.any { it in b } ||
            a.joinToString(" ").contains(b.joinToString(" ")) ||
            b.joinToString(" ").contains(a.joinToString(" "))
    }

    private fun tokenize(name: String): List<String> =
        name.lowercase()
            .split(NON_LETTER_REGEX)
            .filter { it.length >= MIN_TOKEN_LENGTH && it !in NOISE_WORDS }

    private fun levelFor(score: Int): RiskLevel = when {
        score >= DANGEROUS_THRESHOLD -> RiskLevel.DANGEROUS
        score >= SUSPICIOUS_THRESHOLD -> RiskLevel.SUSPICIOUS
        score >= VERIFY_THRESHOLD -> RiskLevel.VERIFY
        else -> RiskLevel.SAFE
    }

    private fun explanation(signals: List<ThreatSignal>, level: RiskLevel): String =
        if (signals.isEmpty()) {
            "All deterministic checks passed: amounts match, merchant identity is consistent."
        } else {
            "${level.name}: " + signals.joinToString(" ") { it.description }
        }

    private fun recommendedAction(level: RiskLevel): String = when (level) {
        RiskLevel.SAFE -> "No issues detected. Proceed as usual."
        RiskLevel.VERIFY -> "Verify the recipient and amount with the payee before authorizing."
        RiskLevel.SUSPICIOUS -> "Details do not add up. Do not authorize payment yet."
        RiskLevel.DANGEROUS -> "Stop immediately. Do not pay, click, or share anything."
    }

    companion object {
        private const val SAFE_BASE_SCORE = 5

        private const val VERIFY_THRESHOLD = 20
        private const val SUSPICIOUS_THRESHOLD = 50
        private const val DANGEROUS_THRESHOLD = 75

        private const val MIN_OPAQUE_HANDLE_LENGTH = 8
        private const val MIN_TOKEN_LENGTH = 3

        private val AMOUNT_NOISE_REGEX = Regex("[^\\d.]")
        private val NON_LETTER_REGEX = Regex("[^a-z]+")

        /** Generic words that carry no identifying power in a business name. */
        private val NOISE_WORDS = setOf("the", "and", "store", "shop", "pay", "ltd", "pvt")
    }
}
