package com.viora.app.core.accessibility

import com.viora.app.domain.perception.AccessibilitySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Perception-layer publication point for the latest accessibility snapshot. */
object AccessibilitySnapshotStore {

    private val _latest = MutableStateFlow<AccessibilitySnapshot?>(null)
    val latest: StateFlow<AccessibilitySnapshot?> = _latest.asStateFlow()

    fun publish(snapshot: AccessibilitySnapshot) {
        _latest.value = snapshot
    }
}