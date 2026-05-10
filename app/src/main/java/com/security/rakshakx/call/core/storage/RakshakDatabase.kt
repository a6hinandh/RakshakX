package com.security.rakshakx.call.core.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import com.security.rakshakx.data.dao.FraudDao
import com.security.rakshakx.data.entities.*

@Database(
    entities = [
        RiskScoreEntity::class,
        SmsEventEntity::class,
        CallEventEntity::class,
        EmailEventEntity::class,
        WebEventEntity::class,
        ThreatSessionEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class RakshakDatabase : RoomDatabase() {
    abstract fun riskScoreDao(): RiskScoreDao
    abstract fun fraudDao(): FraudDao
}
