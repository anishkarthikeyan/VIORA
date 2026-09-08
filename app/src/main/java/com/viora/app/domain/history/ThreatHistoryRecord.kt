package com.viora.app.domain.history

import com.viora.app.domain.model.InputType
import com.viora.app.domain.threat.RiskLevel
import com.viora.app.domain.threat.ThreatSignal

/**
 * A completed security assessment as read back from local history.
 *
 * Plain domain model — carries no persistence-framework annotations, mirroring how
 * [com.viora.app.domain.threat.ThreatAssessment] stays free of Room details. The
 * persistence layer (Entity/DAO/Room) maps into and out of this type; UI and domain
 * code depend only on this and [ThreatHistoryRepository].
 */
data class ThreatHistoryRecord(
    val id: Long,
    val timestamp: Long,
    val inputType: InputType,
    val upiId: String?,
    val merchantName: String?,
    val amount: String?,
    val currency: String?,
    val score: Int,
    val riskLevel: RiskLevel,
    val explanation: String,
    val recommendedAction: String,
    val signals: List<ThreatSignal>
)
