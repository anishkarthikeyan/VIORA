package com.viora.app.domain.history

import com.viora.app.domain.model.VioraContext
import com.viora.app.domain.threat.ThreatAssessment

/**
 * Identifies a "distinct security event" for duplicate-persistence protection
 * (Phase 7). Two calls with the same signature represent the same underlying
 * event — same identifying context fields and same resulting risk/signals — not
 * a new occurrence worth a second history record, even if analysis re-ran because
 * of a debounce/throttle tick (e.g. OCR re-firing on an unchanged camera scene).
 *
 * Pure and Room/Android-free so duplicate-prevention logic is directly
 * unit-testable, independent of ScannerViewModel/coroutines.
 */
fun securityEventSignature(context: VioraContext, assessment: ThreatAssessment): String = listOf(
    context.inputType.name,
    context.upiId,
    context.merchantName,
    context.amount,
    context.currency,
    context.visibleMerchant,
    context.displayedAmount,
    assessment.riskLevel.name,
    assessment.signals.joinToString(",") { it.id }
).joinToString("|") { it.orEmpty() }
