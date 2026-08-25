package com.viora.app.domain.threat

data class ThreatAssessment(
    val score: Int,
    val riskLevel: RiskLevel,
    val signals: List<ThreatSignal> = emptyList(),
    val explanation: String,
    val recommendedAction: String
) {
    companion object {
        /** Empty starting point handed to GuardianAI before analysis. */
        fun neutral(): ThreatAssessment = ThreatAssessment(
            score = 0,
            riskLevel = RiskLevel.SAFE,
            signals = emptyList(),
            explanation = "Awaiting analysis",
            recommendedAction = ""
        )
    }
}
