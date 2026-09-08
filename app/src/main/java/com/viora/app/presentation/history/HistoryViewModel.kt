package com.viora.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viora.app.domain.history.NoOpThreatHistoryRepository
import com.viora.app.domain.history.ThreatHistoryRecord
import com.viora.app.domain.history.ThreatHistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Exposes stored security-analysis history to [HistoryScreen]. Read-only. */
class HistoryViewModel(
    private val repository: ThreatHistoryRepository = NoOpThreatHistoryRepository
) : ViewModel() {

    val history: StateFlow<List<ThreatHistoryRecord>> = repository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}
