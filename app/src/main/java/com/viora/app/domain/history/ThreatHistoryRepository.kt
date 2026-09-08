package com.viora.app.domain.history

import com.viora.app.domain.model.VioraContext
import com.viora.app.domain.threat.ThreatAssessment
import kotlinx.coroutines.flow.Flow

/**
 * Boundary between completed threat analysis and local persistence.
 *
 * ScannerViewModel and other presentation classes depend on this interface only —
 * never on a DAO or Room type directly — so the storage mechanism stays swappable
 * and database details never leak into presentation/domain code.
 */
interface ThreatHistoryRepository {

    /** Persists a completed [assessment] for the [context] that produced it. */
    suspend fun record(context: VioraContext, assessment: ThreatAssessment)

    /** All stored assessments, newest first. */
    fun observeHistory(): Flow<List<ThreatHistoryRecord>>

    /** A single stored assessment, or null if [id] is unknown. */
    suspend fun getById(id: Long): ThreatHistoryRecord?
}
