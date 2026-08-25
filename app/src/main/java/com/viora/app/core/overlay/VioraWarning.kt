package com.viora.app.core.overlay

enum class VioraRiskLevel {
    SAFE,
    VERIFY,
    SUSPICIOUS,
    DANGEROUS
}

data class VioraWarning(
    val title: String,
    val explanation: String,
    val riskLevel: VioraRiskLevel
)