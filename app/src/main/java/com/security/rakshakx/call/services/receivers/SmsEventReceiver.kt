package com.security.rakshakx.call.services.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.security.rakshakx.call.services.foreground.FraudMonitoringForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                val first = messages.firstOrNull()
                val phoneNumber = first?.originatingAddress
                val message = messages.joinToString(separator = "") { it.messageBody ?: "" }.ifBlank { null }

                if (!phoneNumber.isNullOrBlank()) {
                    val orchestrator = FraudMonitoringForegroundService.getOrchestrator(context)
                    orchestrator.handleSmsEvent(phoneNumber, message)
                }
            } catch (exception: Exception) {
                Log.e("SmsEventReceiver", "Failed to handle SMS event", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }
}


