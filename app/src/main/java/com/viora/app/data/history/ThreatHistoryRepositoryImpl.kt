package com.viora.app.data.history

import android.util.Log
import com.viora.app.domain.history.ThreatHistoryRecord
import com.viora.app.domain.history.ThreatHistoryRepository
import com.viora.app.domain.model.VioraContext
import com.viora.app.domain.threat.ThreatAssessment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed [ThreatHistoryRepository]. Sole owner of [ThreatHistoryDao] — presentation
 * code (ScannerViewModel, History UI) depends only on the [ThreatHistoryRepository]
 * interface and never touches the DAO or entity types directly.
 *
 * Persistence failures are caught and logged rather than propagated: a completed
 * analysis must still reach the user (overlay/result screen) even if the local
 * database write fails, per Phase 3's "must not break the user-facing analysis
 * flow" requirement.
 */
class ThreatHistoryRepositoryImpl(
    private val dao: ThreatHistoryDao
) : ThreatHistoryRepository {

    override suspend fun record(context: VioraContext, assessment: ThreatAssessment) {
        try {
            dao.insert(assessment.toHistoryEntity(context))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist threat history entry: ${e.javaClass.simpleName}")
        }
    }

    override fun observeHistory(): Flow<List<ThreatHistoryRecord>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: Long): ThreatHistoryRecord? =
        try {
            dao.getById(id)?.toDomain()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read threat history entry: ${e.javaClass.simpleName}")
            null
        }

    companion object {
        private const val TAG = "ThreatHistoryRepo"
    }
}
