package com.viora.app.ai

import com.viora.app.domain.model.VioraContext
import com.viora.app.domain.threat.ThreatAssessment

interface GuardianAI {
    suspend fun analyze(
        context: VioraContext,
        assessment: ThreatAssessment
    ): ThreatAssessment
}
