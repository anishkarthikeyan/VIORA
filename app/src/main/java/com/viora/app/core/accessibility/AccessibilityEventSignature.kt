package com.viora.app.core.accessibility

import com.viora.app.domain.threat.ThreatAssessment

/**
 * Identifies a "distinct security event" for the accessibility flow (Phase 7):
 * same foreground package + same resulting risk level + same signals means the
 * same underlying event, even if the debounced analysis in
 * [VioraAccessibilityService] re-ran because the visible-text hash changed
 * slightly (a ticking timestamp, a blinking cursor) while the actual scam
 * content on screen did not. Gates both history persistence and the warning
 * overlay so one lingering screen produces exactly one history record and, at
 * most, one overlay.
 *
 * Pure/Android-framework-free so it is directly unit-testable.
 */
fun accessibilityEventSignature(packageName: String, assessment: ThreatAssessment): String =
    "$packageName|${assessment.riskLevel.name}|${assessment.signals.joinToString(",") { it.id }}"
