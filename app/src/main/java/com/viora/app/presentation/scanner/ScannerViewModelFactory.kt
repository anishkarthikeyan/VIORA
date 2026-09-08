package com.viora.app.presentation.scanner

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.viora.app.data.history.ThreatHistoryRepositoryImpl
import com.viora.app.data.history.VioraDatabase

/**
 * Minimal composition-root factory: wires the Room-backed [com.viora.app.domain.history.ThreatHistoryRepository]
 * into [ScannerViewModel] without a dependency-injection framework. Every other
 * ScannerViewModel dependency keeps using its own constructor default.
 */
class ScannerViewModelFactory(private val appContext: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val historyRepository = ThreatHistoryRepositoryImpl(
            VioraDatabase.getInstance(appContext).threatHistoryDao()
        )
        @Suppress("UNCHECKED_CAST")
        return ScannerViewModel(historyRepository = historyRepository) as T
    }
}
