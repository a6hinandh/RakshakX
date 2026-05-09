package com.security.rakshakx.call.services.foreground

import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.security.rakshakx.call.CallMainActivity
import com.security.rakshakx.call.core.storage.DatabaseFactory
import com.security.rakshakx.call.core.storage.RiskScoreRepository

class RiskScanWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    private val threshold = 0.75f

    override suspend fun doWork(): Result {
        return runCatching {
            val db = DatabaseFactory.getInstance(applicationContext)
            val repository = RiskScoreRepository(db.riskScoreDao())
            val topContacts = repository.getTopRiskyContacts(limit = 10)
            val highRisk = topContacts.firstOrNull { it.riskScore >= threshold }

            if (highRisk != null) {
                FraudMonitoringForegroundService.ensureNotificationChannels(applicationContext)
                val message = "RakshakX: High-risk activity detected for ${maskPhone(highRisk.phoneNumber)}. Interact with caution."
                val tapIntent = Intent(applicationContext, CallMainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    applicationContext,
                    3002,
                    tapIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val notification = NotificationCompat.Builder(
                    applicationContext,
                    FraudMonitoringForegroundService.ALERTS_CHANNEL_ID
                )
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle("Security Alert")
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()
                NotificationManagerCompat.from(applicationContext).notify(2002, notification)
            }
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }

    private fun maskPhone(phone: String): String {
        if (phone.length <= 4) return phone
        val visible = phone.takeLast(4)
        return "******$visible"
    }
}


