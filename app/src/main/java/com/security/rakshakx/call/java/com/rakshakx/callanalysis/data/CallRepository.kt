package com.rakshakx.callanalysis.data

import android.content.Context
import androidx.room.Room
import com.rakshakx.callanalysis.RiskConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for call analysis records (renamed from CallRepository for clarity).
 * Manages all database operations for CallRecord entities.
 */
class CallAnalysisRepository(context: Context) {

    private val db = Room.databaseBuilder(
        context.applicationContext,
        CallDatabase::class.java,
        "call_analysis.db"
    ).build()

    private val dao = db.callDao()

    suspend fun saveCallRecord(record: CallRecord) = withContext(Dispatchers.IO) {
        dao.insert(record)
    }

    suspend fun updateCallRecord(record: CallRecord) = withContext(Dispatchers.IO) {
        dao.update(record)
    }

    suspend fun getCallRecordById(id: Long): CallRecord? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    fun getRecentCalls() = dao.getRecentCalls()

    suspend fun getRecentCallsSync() = withContext(Dispatchers.IO) {
        dao.getRecentCallsSync()
    }

    suspend fun getHighRiskCalls() = withContext(Dispatchers.IO) {
        dao.getHighRiskCalls(RiskConfig.THRESHOLD_SAFE_ROUTING)
    }

    suspend fun deleteCallRecord(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }
}

// Legacy alias for compatibility
typealias CallRepository = CallAnalysisRepository


