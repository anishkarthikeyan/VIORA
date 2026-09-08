package com.viora.app.presentation.components

/**
 * Human-readable names for deterministic [com.viora.app.domain.threat.ThreatSignal]
 * ids. Shared by ResultScreen and HistoryScreen so a signal reads identically
 * wherever it appears — previously duplicated between the two screens.
 */
fun signalLabel(id: String): String = when (id) {
    "AMOUNT_MISMATCH" -> "Amount mismatch"
    "MERCHANT_MISMATCH" -> "Merchant mismatch"
    "UNKNOWN_RECIPIENT" -> "Unverified recipient"
    "ACCOUNT_THREAT" -> "Account-threat language"
    "URGENT_ACTION" -> "Urgency pressure"
    "CREDENTIAL_REQUEST" -> "Credential request"
    "OTP_REQUEST" -> "OTP request"
    "PAYMENT_REQUEST" -> "Payment request"
    "FAKE_REWARD" -> "Fake reward / prize claim"
    "SUSPICIOUS_LINK" -> "Suspicious link"
    else -> id.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}
