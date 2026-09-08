package com.viora.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viora.app.domain.history.NoOpThreatHistoryRepository
import com.viora.app.domain.history.ThreatHistoryRecord
import com.viora.app.domain.history.ThreatHistoryRepository
import com.viora.app.domain.threat.RiskLevel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class RecentCheckItem(
    val title: String,
    val subtitle: String,
    val riskLevel: RiskLevel
)

/**
 * Home's "Recent Checks" — the real, Room-backed history (Phase 3), most recent
 * first, trimmed to a small preview. No mock/invented data: an empty repository
 * simply yields an empty list, and HomeScreen renders that as an empty state.
 */
class HomeViewModel(
    repository: ThreatHistoryRepository = NoOpThreatHistoryRepository
) : ViewModel() {

    val recentChecks: StateFlow<List<RecentCheckItem>> = repository.observeHistory()
        .map { history -> history.take(RECENT_PREVIEW_COUNT).map { it.toRecentCheckItem() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private companion object {
        const val RECENT_PREVIEW_COUNT = 4
        const val STOP_TIMEOUT_MS = 5000L
    }
}

private fun ThreatHistoryRecord.toRecentCheckItem(): RecentCheckItem = RecentCheckItem(
    title = signalOrInputTitle(),
    subtitle = (merchantName ?: upiId)?.let { recipient ->
        amount?.let { "$recipient (${currency ?: "₹"}$it)" } ?: recipient
    } ?: inputType.name,
    riskLevel = riskLevel
)

/** First/highest signal name, or the input type when nothing specific fired. */
private fun ThreatHistoryRecord.signalOrInputTitle(): String {
    val topSignal = signals.maxByOrNull { it.score }
    return when {
        topSignal != null -> com.viora.app.presentation.components.signalLabel(topSignal.id)
        riskLevel == RiskLevel.SAFE -> "No issues found"
        else -> "Check completed"
    }
}
