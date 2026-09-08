package com.viora.app.data.history

import androidx.room.TypeConverter

/**
 * Persistence-friendly stand-in for [com.viora.app.domain.threat.ThreatSignal].
 * Room cannot store a `List<ThreatSignal>` column directly, so signals are encoded
 * into one TEXT column via [ThreatSignalListConverter] rather than storing Kotlin
 * objects unsafely. `severity` is kept as the raw enum name (mirrors how
 * [ThreatHistoryEntity.riskLevel] is stored) and resolved back to [com.viora.app.domain.threat.RiskLevel]
 * only at the domain-mapping boundary.
 */
data class ThreatSignalRecord(
    val id: String,
    val score: Int,
    val severity: String,
    val description: String
)

/**
 * Encodes/decodes a list of [ThreatSignalRecord] to a single TEXT column.
 *
 * Uses the ASCII unit-separator (0x1F) and record-separator (0x1E) control
 * characters as delimiters instead of a JSON library, since those characters
 * cannot appear in the plain English signal descriptions ThreatEngine/
 * AccessibilityThreatAnalyzer produce — this avoids adding a new dependency
 * purely for TypeConverter serialization.
 */
class ThreatSignalListConverter {

    @TypeConverter
    fun fromRecords(records: List<ThreatSignalRecord>?): String = encode(records.orEmpty())

    @TypeConverter
    fun toRecords(value: String?): List<ThreatSignalRecord> = decode(value.orEmpty())

    companion object {
        private const val FIELD_SEPARATOR = '\u001F'
        private const val RECORD_SEPARATOR = '\u001E'

        fun encode(records: List<ThreatSignalRecord>): String =
            records.joinToString(RECORD_SEPARATOR.toString()) { record ->
                listOf(record.id, record.score.toString(), record.severity, record.description)
                    .joinToString(FIELD_SEPARATOR.toString())
            }

        fun decode(value: String): List<ThreatSignalRecord> {
            if (value.isEmpty()) return emptyList()
            return value.split(RECORD_SEPARATOR).map { record ->
                val fields = record.split(FIELD_SEPARATOR)
                ThreatSignalRecord(
                    id = fields.getOrElse(0) { "" },
                    score = fields.getOrNull(1)?.toIntOrNull() ?: 0,
                    severity = fields.getOrElse(2) { "" },
                    description = fields.getOrElse(3) { "" }
                )
            }
        }
    }
}
