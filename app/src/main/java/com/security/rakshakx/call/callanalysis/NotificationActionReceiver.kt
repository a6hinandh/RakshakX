package com.security.rakshakx.call.callanalysis

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
 * - View Details
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationActionReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: return
        val phoneNumber = intent.getStringExtra("phoneNumber") ?: return
        val callId = intent.getLongExtra("callId", -1L)

        Log.d(TAG, "Action received: $action for $phoneNumber")

        CoroutineScope(Dispatchers.IO).launch {
            when (action) {
                "ACTION_BLOCK_NUMBER" -> {
                    handleBlockNumber(context, phoneNumber, callId)
                }
                "ACTION_MARK_SAFE" -> {
                    handleMarkSafe(context, callId)
                }
            }
        }

        // Dismiss notification
        if (callId != -1L) {
            NotificationManagerCompat.from(context).cancel(callId.toInt())
        }
    }

    private suspend fun handleBlockNumber(context: Context, phoneNumber: String, callId: Long) {
        try {
            // Add to blocked numbers repository
            val blockedRepository = BlockedNumbersRepository(context)
            blockedRepository.addBlockedNumber(phoneNumber)
            Log.d(TAG, "Number blocked: $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to block number", e)
        }
    }

    private suspend fun handleMarkSafe(context: Context, callId: Long) {
        try {
            // Update call record to ALLOW
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



