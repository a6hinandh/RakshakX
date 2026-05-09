package com.security.rakshakx.notifications.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.security.rakshakx.call.callanalysis.data.BlockedNumbersRepository
import com.security.rakshakx.call.callanalysis.data.CallAnalysisRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles user actions from risk notifications:
 * - Block Number
 * - Mark Safe
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationActionReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: return
        val callId = intent.getLongExtra("callId", -1L)

        when (action) {
            "ACTION_BLOCK_NUMBER" -> {
                val phoneNumber = intent.getStringExtra("phoneNumber") ?: return
                CoroutineScope(Dispatchers.IO).launch {
                    handleBlockNumber(context, phoneNumber, callId)
                }
            }
            "ACTION_MARK_SAFE" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    handleMarkSafe(context, callId)
                }
            }
            else -> return
        }

        if (callId != -1L) {
            NotificationManagerCompat.from(context).cancel(callId.toInt())
        }
    }

    private suspend fun handleBlockNumber(context: Context, phoneNumber: String, callId: Long) {
        try {
            val blockedRepository = BlockedNumbersRepository(context)
            blockedRepository.addBlockedNumber(phoneNumber)
            Log.d(TAG, "Number blocked: $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to block number", e)
        }
    }

    private suspend fun handleMarkSafe(context: Context, callId: Long) {
        try {
            val repository = CallAnalysisRepository(context)
            val record = repository.getCallRecordById(callId)
            if (record != null) {
                val safeRecord = record.copy(
                    action = "ALLOW",
                    reason = "User marked as safe"
                )
                repository.updateCallRecord(safeRecord)
                Log.d(TAG, "Call marked as safe: $callId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark as safe", e)
        }
    }
}
