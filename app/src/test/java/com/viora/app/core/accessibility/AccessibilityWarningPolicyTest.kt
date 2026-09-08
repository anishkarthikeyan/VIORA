package com.viora.app.core.accessibility

import com.viora.app.domain.threat.RiskLevel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityWarningPolicyTest {

    @Test
    fun `does not warn on SAFE`() {
        assertFalse(AccessibilityWarningPolicy.shouldWarn(RiskLevel.SAFE))
    }

    @Test
    fun `does not warn on VERIFY to avoid alert fatigue in the background`() {
        assertFalse(AccessibilityWarningPolicy.shouldWarn(RiskLevel.VERIFY))
    }

    @Test
    fun `warns on SUSPICIOUS`() {
        assertTrue(AccessibilityWarningPolicy.shouldWarn(RiskLevel.SUSPICIOUS))
    }

    @Test
    fun `warns on DANGEROUS`() {
        assertTrue(AccessibilityWarningPolicy.shouldWarn(RiskLevel.DANGEROUS))
    }

    @Test
    fun `does not record SAFE screens`() {
        assertFalse(AccessibilityWarningPolicy.shouldRecord(RiskLevel.SAFE))
    }

    @Test
    fun `records VERIFY even though it does not warn — nothing is silently dropped`() {
        assertTrue(AccessibilityWarningPolicy.shouldRecord(RiskLevel.VERIFY))
        assertFalse(AccessibilityWarningPolicy.shouldWarn(RiskLevel.VERIFY))
    }

    @Test
    fun `records SUSPICIOUS and DANGEROUS`() {
        assertTrue(AccessibilityWarningPolicy.shouldRecord(RiskLevel.SUSPICIOUS))
        assertTrue(AccessibilityWarningPolicy.shouldRecord(RiskLevel.DANGEROUS))
    }
}
