package com.rakshakx.core.storage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RiskScoreEntity::class],
    version = 2,
    exportSchema = false
)
abstract class RakshakDatabase : RoomDatabase() {
    abstract fun riskScoreDao(): RiskScoreDao
}
