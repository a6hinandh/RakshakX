package com.security.rakshakx.call.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RiskScoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: RiskScoreEntity)

    @Query("SELECT * FROM risk_scores WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getByPhoneNumber(phoneNumber: String): RiskScoreEntity?

    @Query("SELECT * FROM risk_scores ORDER BY riskScore DESC LIMIT :limit")
    suspend fun getTopRiskContacts(limit: Int): List<RiskScoreEntity>

    @Query("SELECT * FROM risk_scores ORDER BY riskScore DESC LIMIT :limit")
    fun observeTopRiskContacts(limit: Int): Flow<List<RiskScoreEntity>>

    @Query("SELECT * FROM risk_scores ORDER BY updatedAt DESC")
    fun streamAll(): Flow<List<RiskScoreEntity>>
}


