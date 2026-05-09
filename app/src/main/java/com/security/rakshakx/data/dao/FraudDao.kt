package com.security.rakshakx.data.dao

import androidx.room.*
import com.security.rakshakx.data.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FraudDao {
    // SMS
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSms(event: SmsEventEntity): Long

    @Query("SELECT * FROM sms_events ORDER BY timestamp DESC")
    fun getAllSms(): Flow<List<SmsEventEntity>>

    // CALL
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(event: CallEventEntity): Long

    @Query("SELECT * FROM call_events ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<CallEventEntity>>

    // EMAIL
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmail(event: EmailEventEntity): Long

    @Query("SELECT * FROM email_events ORDER BY timestamp DESC")
    fun getAllEmails(): Flow<List<EmailEventEntity>>

    // WEB
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeb(event: WebEventEntity): Long

    @Query("SELECT * FROM web_events ORDER BY timestamp DESC")
    fun getAllWebEvents(): Flow<List<WebEventEntity>>

    // THREAT SESSIONS
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreatSession(session: ThreatSessionEntity): Long

    @Query("SELECT * FROM threat_sessions WHERE resolved = 0 ORDER BY createdAt DESC")
    fun getActiveThreatSessions(): Flow<List<ThreatSessionEntity>>

    @Query("DELETE FROM sms_events WHERE timestamp < :threshold")
    suspend fun pruneOldSms(threshold: Long)

    @Query("DELETE FROM call_events WHERE timestamp < :threshold")
    suspend fun pruneOldCalls(threshold: Long)

    @Query("SELECT * FROM threat_sessions WHERE overallThreatScore > :minScore")
    fun getSuspiciousSessions(minScore: Float): Flow<List<ThreatSessionEntity>>
}
