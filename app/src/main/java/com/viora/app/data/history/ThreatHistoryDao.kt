package com.viora.app.data.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Minimum DAO surface the current History feature needs. */
@Dao
interface ThreatHistoryDao {

    @Insert
    suspend fun insert(entity: ThreatHistoryEntity): Long

    @Query("SELECT * FROM threat_history ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ThreatHistoryEntity>>

    @Query("SELECT * FROM threat_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ThreatHistoryEntity?
}
