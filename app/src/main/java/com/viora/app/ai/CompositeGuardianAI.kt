package com.viora.app.ai

import com.viora.app.domain.model.VioraContext
import com.viora.app.domain.threat.RiskLevel
import com.viora.app.domain.threat.ThreatAssessment
import com.viora.app.domain.threat.ThreatSignal

/**
 * Orchestrates multiple [GuardianAI] engines (e.g. [ThreatEngine],
 * [AccessibilityThreatAnalyzer], and future ML-based scorers) into one verdict.
 *
 * Runs every engine against the same [VioraContext], pools all [ThreatSignal]s
 * they produce, and recomputes score/riskLevel/explanation/recommendedAction
 * from that combined signal list — mirroring the same scoring/threshold
 * approach used by the individual engines. Each engine's own analysis is
 * unaffected: this class only combines their outputs, it never changes how
 * they compute signals.
 */
class CompositeGuardianAI(
    private val engines: List<GuardianAI>
) : GuardianAI {

    constructor(vararg engines: GuardianAI) : this(engines.toList())

    override suspend fun analyze(
        context: VioraContext,
        assessment: ThreatAssessment
    ): ThreatAssessment {
        val combinedSignals = engines.flatMap { it.analyze(context, assessment).signals }

        val totalScore = combinedSignals.sumOf { it.score }.coerceIn(0, 100)
        val level = levelFor(totalScore)

        return ThreatAssessment(
            score = totalScore,
            riskLevel = level,
            signals = combinedSignals,
            explanation = explanation(combinedSignals, level),
            recommendedAction = recommendedAction(level)
        )
    }

    private fun levelFor(score: Int): RiskLevel = when {
        score >= DANGEROUS_THRESHOLD -> RiskLevel.DANGEROUS
        score >= SUSPICIOUS_THRESHOLD -> RiskLevel.SUSPICIOUS
        score >= VERIFY_THRESHOLD -> RiskLevel.VERIFY
        else -> RiskLevel.SAFE
    }

    private fun explanation(signals: List<ThreatSignal>, level: RiskLevel): String =
        if (signals.isEmpty()) {
            "All engines reported no threat signals."
        } else {
            "${level.name}: " + signals.joinToString(" ") { it.description }
        }

    private fun recommendedAction(level: RiskLevel): String = when (level) {
        RiskLevel.SAFE -> "No issues detected. Proceed as usual."
        RiskLevel.VERIFY -> "Verify the recipient and details before authorizing."
        RiskLevel.SUSPICIOUS -> "Details do not add up. Do not authorize payment yet."
        RiskLevel.DANGEROUS -> "Stop immediately. Do not pay, click, or share anything."
    }

    companion object {
        // Mirrors the thresholds used by ThreatEngine and AccessibilityThreatAnalyzer
        // so a combined verdict classifies consistently with either engine alone.
        private const val VERIFY_THRESHOLD = 20
        private const val SUSPICIOUS_THRESHOLD = 50
        private const val DANGEROUS_THRESHOLD = 75
    }
}
