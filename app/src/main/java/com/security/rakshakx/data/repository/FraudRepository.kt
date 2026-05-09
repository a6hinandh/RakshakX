package com.security.rakshakx.data.repository

import com.security.rakshakx.data.dao.FraudDao
import com.security.rakshakx.data.entities.*
import kotlinx.coroutines.flow.Flow

class FraudRepository(private val fraudDao: FraudDao) {
    
    val allSms: Flow<List<SmsEventEntity>> = fraudDao.getAllSms()
    val allCalls: Flow<List<CallEventEntity>> = fraudDao.getAllCalls()
    val allEmails: Flow<List<EmailEventEntity>> = fraudDao.getAllEmails()
    val allWebEvents: Flow<List<WebEventEntity>> = fraudDao.getAllWebEvents()
    val activeThreatSessions: Flow<List<ThreatSessionEntity>> = fraudDao.getActiveThreatSessions()

    suspend fun logSms(event: SmsEventEntity) = fraudDao.insertSms(event)
    suspend fun logCall(event: CallEventEntity) = fraudDao.insertCall(event)
    suspend fun logEmail(event: EmailEventEntity) = fraudDao.insertEmail(event)
    suspend fun logWebEvent(event: WebEventEntity) = fraudDao.insertWeb(event)
    suspend fun logThreatSession(session: ThreatSessionEntity) = fraudDao.insertThreatSession(session)

    suspend fun cleanup(days: Int = 30) {
        val threshold = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        fraudDao.pruneOldSms(threshold)
        fraudDao.pruneOldCalls(threshold)
    }
}
