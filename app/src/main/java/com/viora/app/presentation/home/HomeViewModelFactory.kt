package com.viora.app.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.viora.app.data.history.ThreatHistoryRepositoryImpl
import com.viora.app.data.history.VioraDatabase

/** Composition-root factory wiring the Room-backed repository into [HomeViewModel]. */
class HomeViewModelFactory(private val appContext: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val historyRepository = ThreatHistoryRepositoryImpl(
            VioraDatabase.getInstance(appContext).threatHistoryDao()
        )
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(historyRepository) as T
    }
}
