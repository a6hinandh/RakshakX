package com.security.rakshakx.email.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ThreatDao {

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertThreat(
        threat: ThreatEntity
    )

    @Query(
        "SELECT * FROM threats ORDER BY timestamp DESC"
    )
    suspend fun getAllThreats(): List<ThreatEntity>
}