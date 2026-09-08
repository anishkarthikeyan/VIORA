package com.viora.app.data.history

import com.viora.app.domain.history.ThreatHistoryRecord
import com.viora.app.domain.model.InputType
import com.viora.app.domain.model.VioraContext
import com.viora.app.domain.threat.RiskLevel
import com.viora.app.domain.threat.ThreatAssessment
import com.viora.app.domain.threat.ThreatSignal

/**
 * Pure domain <-> persistence mapping for threat history. Kept as free functions
 * (no Room/Android dependency) so mapping correctness is unit-testable on its own,
 * independent of an actual database.
 */

/** Builds the entity to persist for a completed [assessment] of [context]. */
fun ThreatAssessment.toHistoryEntity(
    context: VioraContext,
    timestamp: Long = System.currentTimeMillis()
): ThreatHistoryEntity = ThreatHistoryEntity(
    timestamp = timestamp,
    inputType = context.inputType.name,
    upiId = context.upiId,
    merchantName = context.merchantName,
    amount = context.amount,
    currency = context.currency,
    score = score,
    riskLevel = riskLevel.name,
    explanation = explanation,
    recommendedAction = recommendedAction,
    signals = signals.map { it.toRecord() }
)

fun ThreatSignal.toRecord(): ThreatSignalRecord =
    ThreatSignalRecord(id = id, score = score, severity = severity.name, description = description)

fun ThreatSignalRecord.toDomain(): ThreatSignal =
    ThreatSignal(
        id = id,
        score = score,
        severity = severity.toRiskLevelOrDefault(),
        description = description
    )

/** Reconstructs a display-ready [ThreatHistoryRecord] from a stored entity. */
fun ThreatHistoryEntity.toDomain(): ThreatHistoryRecord = ThreatHistoryRecord(
    id = id,
    timestamp = timestamp,
    inputType = inputType.toInputTypeOrDefault(),
    upiId = upiId,
    merchantName = merchantName,
    amount = amount,
    currency = currency,
    score = score,
    riskLevel = riskLevel.toRiskLevelOrDefault(),
    explanation = explanation,
    recommendedAction = recommendedAction,
    signals = signals.map { it.toDomain() }
)

private fun String.toRiskLevelOrDefault(): RiskLevel =
    RiskLevel.entries.firstOrNull { it.name == this } ?: RiskLevel.SAFE

private fun String.toInputTypeOrDefault(): InputType =
    InputType.entries.firstOrNull { it.name == this } ?: InputType.TEXT
