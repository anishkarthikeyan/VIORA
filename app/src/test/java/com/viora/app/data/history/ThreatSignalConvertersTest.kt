package com.viora.app.data.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreatSignalConvertersTest {

    private val converter = ThreatSignalListConverter()

    @Test
    fun `round-trips an empty list`() {
        val encoded = converter.fromRecords(emptyList())
        assertEquals("", encoded)
        assertTrue(converter.toRecords(encoded).isEmpty())
    }

    @Test
    fun `round-trips a null list as empty`() {
        assertTrue(converter.toRecords(null).isEmpty())
    }

    @Test
    fun `round-trips a single signal`() {
        val records = listOf(
            ThreatSignalRecord(
                id = "AMOUNT_MISMATCH",
                score = 35,
                severity = "SUSPICIOUS",
                description = "Amount mismatch: screen shows ₹850 but the payload requests ₹5000."
            )
        )

        val decoded = converter.toRecords(converter.fromRecords(records))

        assertEquals(records, decoded)
    }

    @Test
    fun `round-trips multiple signals preserving order and every field`() {
        val records = listOf(
            ThreatSignalRecord("ACCOUNT_THREAT", 50, "SUSPICIOUS", "The message threatens account access."),
            ThreatSignalRecord("URGENT_ACTION", 20, "VERIFY", "The message pressures immediate action."),
            ThreatSignalRecord("OTP_REQUEST", 50, "SUSPICIOUS", "The message requests a one-time password.")
        )

        val decoded = converter.toRecords(converter.fromRecords(records))

        assertEquals(records, decoded)
        assertEquals(records.map { it.id }, decoded.map { it.id })
    }
}
