package com.viora.app.ai

import com.viora.app.domain.model.InputType
import com.viora.app.domain.model.VioraContext
import com.viora.app.domain.threat.RiskLevel
import com.viora.app.domain.threat.ThreatAssessment
import com.viora.app.domain.threat.ThreatSignal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositeGuardianAITest {

    private val context = VioraContext(inputType = InputType.CAMERA, rawContent = "test")

    /** Minimal GuardianAI stub that always returns a fixed set of signals. */
    private fun stubEngine(vararg signals: ThreatSignal): GuardianAI = object : GuardianAI {
        override suspend fun analyze(
            context: VioraContext,
            assessment: ThreatAssessment
        ): ThreatAssessment = ThreatAssessment(
            score = signals.sumOf { it.score }.coerceIn(0, 100),
            riskLevel = RiskLevel.SAFE, // irrelevant: CompositeGuardianAI recomputes this
            signals = signals.toList(),
            explanation = "stub",
            recommendedAction = "stub"
        )
    }

    private fun analyze(composite: CompositeGuardianAI): ThreatAssessment = runBlocking {
        composite.analyze(context, ThreatAssessment.neutral())
    }

    @Test
    fun `combines signals from two engines with different signals`() {
        val engineA = stubEngine(
            ThreatSignal(id = "SIGNAL_A", score = 30, severity = RiskLevel.VERIFY, description = "signal A fired")
        )
        val engineB = stubEngine(
            ThreatSignal(id = "SIGNAL_B", score = 25, severity = RiskLevel.SUSPICIOUS, description = "signal B fired")
        )

        val result = analyze(CompositeGuardianAI(listOf(engineA, engineB)))

        assertEquals(setOf("SIGNAL_A", "SIGNAL_B"), result.signals.map { it.id }.toSet())
        assertEquals(55, result.score) // 30 + 25
        assertEquals(RiskLevel.SUSPICIOUS, result.riskLevel)
        assertTrue(result.explanation.contains("signal A fired"))
        assertTrue(result.explanation.contains("signal B fired"))
    }

    @Test
    fun `empty engine list returns SAFE`() {
        val result = analyze(CompositeGuardianAI(emptyList()))

        assertEquals(0, result.score)
        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertTrue(result.signals.isEmpty())
    }

    @Test
    fun `combined score is clamped at 100`() {
        val engineA = stubEngine(
            ThreatSignal(id = "SIGNAL_A", score = 80, severity = RiskLevel.DANGEROUS, description = "signal A fired")
        )
        val engineB = stubEngine(
            ThreatSignal(id = "SIGNAL_B", score = 60, severity = RiskLevel.DANGEROUS, description = "signal B fired")
        )

        val result = analyze(CompositeGuardianAI(listOf(engineA, engineB)))

        assertEquals(100, result.score) // 80 + 60 = 140, clamped to 100
        assertEquals(RiskLevel.DANGEROUS, result.riskLevel)
    }
}
