package com.viora.app.data.history

import com.viora.app.domain.model.InputType
import com.viora.app.domain.model.VioraContext
import com.viora.app.domain.threat.RiskLevel
import com.viora.app.domain.threat.ThreatAssessment
import com.viora.app.domain.threat.ThreatSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreatHistoryMapperTest {

    private val context = VioraContext(
        inputType = InputType.QR,
        rawContent = "upi://pay?pa=merchant@bank&am=5000",
        upiId = "merchant@bank",
        merchantName = "Merchant Co",
        amount = "5000",
        currency = "INR",
        extractedText = "Please share the OTP now" // must NOT be persisted (privacy)
    )

    private val assessment = ThreatAssessment(
        score = 55,
        riskLevel = RiskLevel.SUSPICIOUS,
        signals = listOf(
            ThreatSignal("AMOUNT_MISMATCH", 35, RiskLevel.SUSPICIOUS, "Amount mismatch."),
            ThreatSignal("UNKNOWN_RECIPIENT", 20, RiskLevel.VERIFY, "Recipient unverified.")
        ),
        explanation = "SUSPICIOUS: Amount mismatch. Recipient unverified.",
        recommendedAction = "Do not authorize payment yet."
    )

    @Test
    fun `maps assessment and context fields into the entity`() {
        val entity = assessment.toHistoryEntity(context, timestamp = 1_000L)

        assertEquals(1_000L, entity.timestamp)
        assertEquals("QR", entity.inputType)
        assertEquals("merchant@bank", entity.upiId)
        assertEquals("Merchant Co", entity.merchantName)
        assertEquals("5000", entity.amount)
        assertEquals("INR", entity.currency)
        assertEquals(55, entity.score)
        assertEquals("SUSPICIOUS", entity.riskLevel)
        assertEquals(assessment.explanation, entity.explanation)
        assertEquals(assessment.recommendedAction, entity.recommendedAction)
    }

    @Test
    fun `does not persist raw or extracted text`() {
        val entity = assessment.toHistoryEntity(context, timestamp = 1_000L)

        // The entity has no property carrying rawContent/extractedText at all —
        // this test documents that guarantee by reflecting over the entity's fields.
        val fieldNames = ThreatHistoryEntity::class.java.declaredFields.map { it.name }
        assertTrue(fieldNames.none { it.contains("raw", ignoreCase = true) })
        assertTrue(fieldNames.none { it.contains("extractedText", ignoreCase = true) })
    }

    @Test
    fun `preserves every signal through the round trip`() {
        val entity = assessment.toHistoryEntity(context, timestamp = 1_000L)
        val record = entity.toDomain()

        assertEquals(assessment.signals.size, record.signals.size)
        assertEquals(assessment.signals, record.signals)
    }

    @Test
    fun `round-trips entity back to a domain record`() {
        val entity = assessment.toHistoryEntity(context, timestamp = 42L).copy(id = 7L)

        val record = entity.toDomain()

        assertEquals(7L, record.id)
        assertEquals(42L, record.timestamp)
        assertEquals(InputType.QR, record.inputType)
        assertEquals(context.upiId, record.upiId)
        assertEquals(context.merchantName, record.merchantName)
        assertEquals(context.amount, record.amount)
        assertEquals(context.currency, record.currency)
        assertEquals(assessment.score, record.score)
        assertEquals(assessment.riskLevel, record.riskLevel)
        assertEquals(assessment.explanation, record.explanation)
        assertEquals(assessment.recommendedAction, record.recommendedAction)
    }

    @Test
    fun `falls back to a safe default for an unrecognized stored risk level`() {
        val entity = assessment.toHistoryEntity(context, timestamp = 1L).copy(riskLevel = "NOT_A_REAL_LEVEL")

        assertEquals(RiskLevel.SAFE, entity.toDomain().riskLevel)
    }

    @Test
    fun `omits optional fields that were absent on the context`() {
        val bareContext = VioraContext(inputType = InputType.TEXT, rawContent = "text")
        val neutral = ThreatAssessment.neutral()

        val entity = neutral.toHistoryEntity(bareContext, timestamp = 1L)

        assertNull(entity.upiId)
        assertNull(entity.merchantName)
        assertNull(entity.amount)
        assertNull(entity.currency)
    }
}
