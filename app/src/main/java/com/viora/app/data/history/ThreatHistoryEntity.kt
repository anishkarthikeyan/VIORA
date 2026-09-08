package com.viora.app.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

/**
 * Room persistence record for one completed [com.viora.app.domain.threat.ThreatAssessment].
 *
 * Deliberately NOT the domain model — [com.viora.app.domain.threat.ThreatAssessment] and
 * [com.viora.app.domain.model.VioraContext] stay free of Room annotations. Mapping to/from
 * this entity lives in [ThreatHistoryMapper].
 *
 * Data minimization (see Phase 3 privacy requirements): this entity intentionally does NOT
 * store raw scanned/shared text, OCR output, or full accessibility snapshots — only the
 * structured fields needed to explain a past assessment (recipient/amount/merchant
 * metadata, score, risk level, explanation, recommended action, and signals). Free-form
 * `extractedText`/`rawContent` from [com.viora.app.domain.model.VioraContext] is never
 * persisted, since it can incidentally contain OTPs, PINs, or other sensitive on-screen
 * text and the History feature does not need it to explain a verdict.
 */
@Entity(tableName = "threat_history")
data class ThreatHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    /** [com.viora.app.domain.model.InputType] enum name. */
    val inputType: String,
    val upiId: String?,
    val merchantName: String?,
    val amount: String?,
    val currency: String?,
    val score: Int,
    /** [com.viora.app.domain.threat.RiskLevel] enum name. */
    val riskLevel: String,
    val explanation: String,
    val recommendedAction: String,
    @param:TypeConverters(ThreatSignalListConverter::class)
    val signals: List<ThreatSignalRecord>
)
