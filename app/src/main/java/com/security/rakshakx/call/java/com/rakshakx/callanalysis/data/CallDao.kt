package com.rakshakx.callanalysis.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO for CallRecord operations.
 */
@Dao
interface CallDao {
    @Insert
    suspend fun insert(record: CallRecord)

    @Update
    suspend fun update(record: CallRecord)

    @Query("SELECT * FROM call_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CallRecord?

    @Query("SELECT * FROM call_records ORDER BY timestamp DESC LIMIT 50")
    fun getRecentCalls(): Flow<List<CallRecord>>

    @Query("SELECT * FROM call_records ORDER BY timestamp DESC LIMIT 50")
    suspend fun getRecentCallsSync(): List<CallRecord>

    @Query("SELECT * FROM call_records WHERE riskScore >= :threshold ORDER BY timestamp DESC")
    suspend fun getHighRiskCalls(threshold: Float): List<CallRecord>

    @Query("DELETE FROM call_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM call_records")
    suspend fun getTotalCount(): Int

    @Query("SELECT AVG(riskScore) FROM call_records")
    suspend fun getAverageRiskScore(): Float

    // NEW: get the most recent call
    @Query("SELECT * FROM call_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastCall(): CallRecord?
}