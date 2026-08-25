package com.viora.app.domain.threat

data class ThreatSignal(
    val id: String,
    val score: Int,
    val severity: RiskLevel,
    val description: String
)
