package com.viora.app.domain.perception

/** Structured text collected from the active accessibility tree. */
data class AccessibilitySnapshot(
    val packageName: String,
    val timestamp: Long,
    val visibleText: String
)