package com.viora.app.ai

import com.viora.app.domain.perception.AccessibilitySnapshot
import com.viora.app.domain.threat.RiskLevel
import com.viora.app.domain.threat.ThreatAssessment
import com.viora.app.domain.threat.ThreatSignal

/**
 * Small, deterministic classifier for text collected by AccessibilityService.
 *
 * This is deliberately separate from [ThreatEngine] so it can later be replaced
 * by a model without changing the existing payment analysis contract.
 */
class AccessibilityThreatAnalyzer {

    fun analyze(snapshot: AccessibilitySnapshot): ThreatAssessment =
        analyze(snapshot.visibleText)

    fun analyze(text: String): ThreatAssessment {
        val normalized = text.lowercase().replace(WHITESPACE_REGEX, " ").trim()
        val signals = buildList {
            addIf(ACCOUNT_THREAT, normalized.containsAccountThreat())
            addIf(URGENT_ACTION, normalized.containsUrgentAction())
            addIf(CREDENTIAL_REQUEST, normalized.containsCredentialRequest())
            addIf(OTP_REQUEST, normalized.containsOtpRequest())
            addIf(PAYMENT_REQUEST, normalized.containsPaymentRequest())
            addIf(FAKE_REWARD, normalized.containsFakeReward())
            addIf(SUSPICIOUS_LINK, normalized.containsSuspiciousLink())
        }

        val score = signals.sumOf { it.score }.coerceIn(0, 100)
        val level = when {
            score >= DANGEROUS_THRESHOLD -> RiskLevel.DANGEROUS
            score >= SUSPICIOUS_THRESHOLD -> RiskLevel.SUSPICIOUS
            score >= VERIFY_THRESHOLD -> RiskLevel.VERIFY
            else -> RiskLevel.SAFE
        }

        return ThreatAssessment(
            score = score,
            riskLevel = level,
            signals = signals,
            explanation = explanation(signals, level),
            recommendedAction = actionFor(level)
        )
    }

    private fun MutableList<ThreatSignal>.addIf(signal: ThreatSignal, matched: Boolean) {
        if (matched) add(signal)
    }

    private fun explanation(signals: List<ThreatSignal>, level: RiskLevel): String =
        if (signals.isEmpty()) {
            "No high-confidence social-engineering pattern was detected."
        } else {
            "${level.name}: " + signals.joinToString(" ") { it.description }
        }

    private fun actionFor(level: RiskLevel) = when (level) {
        RiskLevel.SAFE -> "No action is required."
        RiskLevel.VERIFY -> "Verify this request through an independent channel."
        RiskLevel.SUSPICIOUS -> "Pause and verify before clicking, paying, or sharing information."
        RiskLevel.DANGEROUS -> "Stop immediately. Do not click, pay, or share credentials."
    }

    private fun String.containsAccountThreat(): Boolean =
        ACCOUNT_THREAT_REGEX.containsMatchIn(this)

    private fun String.containsUrgentAction(): Boolean =
        URGENT_ACTION_REGEX.containsMatchIn(this) && URGENT_CONTEXT_REGEX.containsMatchIn(this)

    private fun String.containsCredentialRequest(): Boolean =
        CREDENTIAL_REQUEST_REGEX.containsMatchIn(this)

    private fun String.containsOtpRequest(): Boolean =
        OTP_REQUEST_REGEX.containsMatchIn(this)

    private fun String.containsPaymentRequest(): Boolean =
        PAYMENT_REQUEST_REGEX.containsMatchIn(this)

    private fun String.containsFakeReward(): Boolean =
        REWARD_REGEX.containsMatchIn(this) && PAYMENT_OR_CLAIM_REGEX.containsMatchIn(this)

    private fun String.containsSuspiciousLink(): Boolean =
        LINK_REGEX.containsMatchIn(this) && LINK_CONTEXT_REGEX.containsMatchIn(this)

    companion object {
        private val ACCOUNT_THREAT = ThreatSignal(
            id = "ACCOUNT_THREAT",
            score = 50,
            severity = RiskLevel.SUSPICIOUS,
            description = "The message threatens account access or suspension."
        )
        private val URGENT_ACTION = ThreatSignal(
            id = "URGENT_ACTION",
            score = 20,
            severity = RiskLevel.VERIFY,
            description = "The message pressures immediate action."
        )
        private val CREDENTIAL_REQUEST = ThreatSignal(
            id = "CREDENTIAL_REQUEST",
            score = 45,
            severity = RiskLevel.SUSPICIOUS,
            description = "The message requests sensitive credentials."
        )
        private val OTP_REQUEST = ThreatSignal(
            id = "OTP_REQUEST",
            score = 50,
            severity = RiskLevel.SUSPICIOUS,
            description = "The message requests a one-time password."
        )
        private val PAYMENT_REQUEST = ThreatSignal(
            id = "PAYMENT_REQUEST",
            score = 35,
            severity = RiskLevel.SUSPICIOUS,
            description = "The message requests a payment or transfer."
        )
        private val FAKE_REWARD = ThreatSignal(
            id = "FAKE_REWARD",
            score = 40,
            severity = RiskLevel.SUSPICIOUS,
            description = "The message pairs a reward with a payment or claim request."
        )
        private val SUSPICIOUS_LINK = ThreatSignal(
            id = "SUSPICIOUS_LINK",
            score = 25,
            severity = RiskLevel.SUSPICIOUS,
            description = "The message links an account action to an external link."
        )

        private const val VERIFY_THRESHOLD = 20
        private const val SUSPICIOUS_THRESHOLD = 50
        private const val DANGEROUS_THRESHOLD = 75

        private val WHITESPACE_REGEX = Regex("\\s+")
        private val ACCOUNT_THREAT_REGEX = Regex(
            "\\b(account|profile|wallet|kyc)\\b.{0,45}\\b(blocked|suspended|disabled|locked|deactivated|terminated)\\b"
        )
        private val URGENT_ACTION_REGEX = Regex(
            "\\b(verify|confirm|update|click|act|respond|pay|share)\\b.{0,35}\\b(immediately|urgent(?:ly)?|now|within)\\b"
        )
        private val URGENT_CONTEXT_REGEX = Regex(
            "\\b(account|profile|wallet|kyc|payment|password|pin|otp|code|credentials)\\b"
        )
        private val CREDENTIAL_REQUEST_REGEX = Regex(
            "\\b(share|send|provide|enter|confirm|submit)\\b.{0,25}\\b(password|passcode|pin|card details|login details|credentials)\\b"
        )
        private val OTP_REQUEST_REGEX = Regex(
            "\\b(share|send|provide|tell|enter|forward)\\b.{0,20}\\b(otp|one[- ]time password|verification code)\\b"
        )
        private val PAYMENT_REQUEST_REGEX = Regex(
            "\\b(pay|transfer|send|deposit|fee|payment)\\b.{0,35}(?:₹|rs\\.?|inr|\\b\\d{2,}\\b)"
        )
        private val REWARD_REGEX = Regex("\\b(reward|cashback|prize|bonus|refund|winner|won)\\b")
        private val PAYMENT_OR_CLAIM_REGEX = Regex(
            "\\b(pay|transfer|send|deposit|fee|payment|claim|collect|activate)\\b"
        )
        private val LINK_REGEX = Regex("(?:https?://|www\\.|\\blink\\b)")
        private val LINK_CONTEXT_REGEX = Regex(
            "\\b(verify|confirm|update|unlock|secure|account|kyc|login)\\b"
        )
    }
}