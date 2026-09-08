package com.viora.app.data.history

import com.viora.app.domain.model.InputType
import com.viora.app.domain.model.VioraContext
import com.viora.app.domain.threat.RiskLevel
import com.viora.app.domain.threat.ThreatAssessment
import com.viora.app.domain.threat.ThreatSignal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * In-memory fake standing in for Room — lets repository behavior (mapping,
 * ordering, error handling) be verified without a real database or Robolectric.
 */
private class FakeThreatHistoryDao(
    private val failOnInsert: Boolean = false
) : ThreatHistoryDao {
    private val stored = MutableStateFlow<List<ThreatHistoryEntity>>(emptyList())
    private var nextId = 1L

    override suspend fun insert(entity: ThreatHistoryEntity): Long {
        if (failOnInsert) throw IllegalStateException("simulated database failure")
        val withId = entity.copy(id = nextId++)
        // Newest first, mirroring the real @Query("... ORDER BY timestamp DESC").
        stored.value = (stored.value + withId).sortedByDescending { it.timestamp }
        return withId.id
    }

    override fun observeAll(): Flow<List<ThreatHistoryEntity>> = stored

    override suspend fun getById(id: Long): ThreatHistoryEntity? = stored.value.firstOrNull { it.id == id }
}

class ThreatHistoryRepositoryImplTest {

    private val context = VioraContext(inputType = InputType.QR, rawContent = "upi://pay?pa=a@bank")

    private fun assessment(score: Int, level: RiskLevel) = ThreatAssessment(
        score = score,
        riskLevel = level,
        signals = listOf(ThreatSignal("SIGNAL", score, level, "desc")),
        explanation = "explanation",
        recommendedAction = "action"
    )

    @Test
    fun `record maps and inserts a completed assessment`() = runBlocking {
        val dao = FakeThreatHistoryDao()
        val repository = ThreatHistoryRepositoryImpl(dao)

        repository.record(context, assessment(60, RiskLevel.SUSPICIOUS))

        val history = repository.observeHistory().first()
        assertEquals(1, history.size)
        assertEquals(60, history.first().score)
        assertEquals(RiskLevel.SUSPICIOUS, history.first().riskLevel)
        assertEquals(context.upiId, history.first().upiId)
    }

    @Test
    fun `observeHistory returns newest first`() = runBlocking {
        val dao = FakeThreatHistoryDao()
        val repository = ThreatHistoryRepositoryImpl(dao)

        // Explicit timestamps (rather than relying on System.currentTimeMillis()
        // across two quick calls) so ordering is deterministic.
        dao.insert(assessment(10, RiskLevel.SAFE).toHistoryEntity(context, timestamp = 1_000L))
        dao.insert(assessment(80, RiskLevel.DANGEROUS).toHistoryEntity(context, timestamp = 2_000L))

        val history = repository.observeHistory().first()
        assertEquals(listOf(80, 10), history.map { it.score })
    }

    @Test
    fun `getById returns a stored record by id`() = runBlocking {
        val dao = FakeThreatHistoryDao()
        val repository = ThreatHistoryRepositoryImpl(dao)
        repository.record(context, assessment(45, RiskLevel.VERIFY))

        val storedId = repository.observeHistory().first().first().id
        val fetched = repository.getById(storedId)

        assertEquals(45, fetched?.score)
    }

    @Test
    fun `getById returns null for an unknown id`() = runBlocking {
        val repository = ThreatHistoryRepositoryImpl(FakeThreatHistoryDao())

        assertNull(repository.getById(999L))
    }

    @Test
    fun `a failed insert does not throw — persistence errors are swallowed`() = runBlocking {
        val dao = FakeThreatHistoryDao(failOnInsert = true)
        val repository = ThreatHistoryRepositoryImpl(dao)

        // Must not throw: a broken database must never break the analysis flow.
        repository.record(context, assessment(70, RiskLevel.DANGEROUS))

        assertTrue(repository.observeHistory().first().isEmpty())
    }
}
