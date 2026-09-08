package com.viora.app.data.history

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Local, offline-only Room database for security-analysis history.
 *
 * Initialization is a plain double-checked-locking singleton keyed off the
 * Application context — no dependency-injection framework, matching how
 * [com.viora.app.core.accessibility.VioraAccessibilityService] and
 * [com.viora.app.presentation.scanner.ScannerViewModel] already get their
 * dependencies via simple constructor defaults/factories.
 */
@Database(entities = [ThreatHistoryEntity::class], version = 1, exportSchema = false)
@TypeConverters(ThreatSignalListConverter::class)
abstract class VioraDatabase : RoomDatabase() {

    abstract fun threatHistoryDao(): ThreatHistoryDao

    companion object {
        private const val DATABASE_NAME = "viora_security_history.db"

        @Volatile
        private var instance: VioraDatabase? = null

        fun getInstance(context: Context): VioraDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VioraDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
    }
}
