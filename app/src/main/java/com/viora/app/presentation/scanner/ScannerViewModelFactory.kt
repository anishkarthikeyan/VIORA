package com.viora.app.presentation.scanner

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.viora.app.ai.AccessibilityThreatAnalyzer
import com.viora.app.ai.CompositeGuardianAI
import com.viora.app.ai.MLGuardianAI
import com.viora.app.ai.ThreatEngine
import com.viora.app.ai.ml.LocalThreatClassifier
import com.viora.app.data.history.ThreatHistoryRepositoryImpl
import com.viora.app.data.history.VioraDatabase

/**
 * Minimal composition-root factory: wires the Room-backed [com.viora.app.domain.history.ThreatHistoryRepository]
 * and the real application-level [CompositeGuardianAI] (deterministic engines +
 * Phase 8's on-device [MLGuardianAI]) into [ScannerViewModel] without a
 * dependency-injection framework. Every other ScannerViewModel dependency keeps
 * using its own constructor default.
 */
class ScannerViewModelFactory(private val appContext: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val historyRepository = ThreatHistoryRepositoryImpl(
            VioraDatabase.getInstance(appContext).threatHistoryDao()
        )
        val guardianAI = CompositeGuardianAI(
            ThreatEngine(),
            AccessibilityThreatAnalyzer(),
            MLGuardianAI(LocalThreatClassifier(appContext))
        )
        @Suppress("UNCHECKED_CAST")
        return ScannerViewModel(guardianAI = guardianAI, historyRepository = historyRepository) as T
    }
}
