package com.viora.app.domain.history

import com.viora.app.domain.model.VioraContext
import com.viora.app.domain.threat.ThreatAssessment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Safe default [ThreatHistoryRepository] used when no real persistence has been
 * wired in (e.g. plain `ScannerViewModel()` construction in tests/previews, where
 * no Context is available to open the Room database). Recording is a no-op;
 * history reads back empty. Keeps ScannerViewModel constructible without Room.
 */
object NoOpThreatHistoryRepository : ThreatHistoryRepository {
    override suspend fun record(context: VioraContext, assessment: ThreatAssessment) {
        // Intentionally does nothing — no persistence has been configured.
    }

    override fun observeHistory(): Flow<List<ThreatHistoryRecord>> = flowOf(emptyList())

    override suspend fun getById(id: Long): ThreatHistoryRecord? = null
}
