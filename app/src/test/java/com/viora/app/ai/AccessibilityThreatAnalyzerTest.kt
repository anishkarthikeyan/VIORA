package com.viora.app.ai

import com.viora.app.domain.perception.AccessibilitySnapshot
import com.viora.app.domain.threat.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityThreatAnalyzerTest {

    private val analyzer = AccessibilityThreatAnalyzer()

    @Test
    fun `urgent alone does not produce a warning`() {
        val result = analyzer.analyze("Act urgently to avoid delays")

        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertTrue(result.signals.isEmpty())
    }

    @Test
    fun `account blocked is suspicious`() {
        val result = analyzer.analyze("Your account will be blocked")

        assertEquals(RiskLevel.SUSPICIOUS, result.riskLevel)
        assertTrue(result.signals.any { it.id == "ACCOUNT_THREAT" })
    }

    @Test
    fun `account threat urgent action and link are dangerous`() {
        val result = analyzer.analyze(
            "Your account will be blocked. Verify immediately. Click this link https://example.com to verify your account."
        )

        assertEquals(RiskLevel.DANGEROUS, result.riskLevel)
        assertEquals(
            setOf("ACCOUNT_THREAT", "URGENT_ACTION", "SUSPICIOUS_LINK"),
            result.signals.map { it.id }.toSet()
        )
    }

    @Test
    fun `otp request is suspicious`() {
        val result = analyzer.analyze("Share your OTP")

        assertEquals(RiskLevel.SUSPICIOUS, result.riskLevel)
        assertTrue(result.signals.any { it.id == "OTP_REQUEST" })
    }

    @Test
    fun `reward payment is dangerous`() {
        val result = analyzer.analyze("Pay ₹500 to receive your reward")

        assertEquals(RiskLevel.DANGEROUS, result.riskLevel)
        assertTrue(result.signals.any { it.id == "PAYMENT_REQUEST" })
        assertTrue(result.signals.any { it.id == "FAKE_REWARD" })
    }

    @Test
    fun `snapshot analysis uses only its visible text`() {
        val result = analyzer.analyze(
            AccessibilitySnapshot("com.example.bank", 123L, "Verify your KYC immediately")
        )

        assertEquals(RiskLevel.VERIFY, result.riskLevel)
        assertTrue(result.signals.any { it.id == "URGENT_ACTION" })
    }
}