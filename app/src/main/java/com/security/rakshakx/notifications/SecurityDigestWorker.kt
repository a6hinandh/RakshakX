package com.security.rakshakx.notifications

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.security.rakshakx.call.core.storage.DatabaseFactory
import com.security.rakshakx.core.SettingsStore
import com.security.rakshakx.core.threatintel.ThreatIntelligenceManager
import com.security.rakshakx.ui.data.ThreatLogRepository
import com.security.rakshakx.permissions.PermissionManager
import java.util.concurrent.TimeUnit

class SecurityDigestWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SecurityDigest"
        private const val DIGEST_NOTIFICATION_ID = 9500
        private const val WORK_NAME = "security_digest"

        fun scheduleDaily(context: Context) {
            val request = PeriodicWorkRequestBuilder<SecurityDigestWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(1, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "Security digest scheduled")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val context = applicationContext
            
            // Daily automated logs cleanup based on settings retention period
            try {
                val settingsStore = SettingsStore.getInstance(context)
                val autoDeleteDaysVal = settingsStore.autoDeleteDays.value
                ThreatLogRepository.cleanOldLogs(context, autoDeleteDaysVal)
            } catch (e: Exception) {
                Log.w(TAG, "Data retention automated cleanup failed: ${e.message}")
            }

            // Generate threat intel report from recent flagged data
            try {
                val threatIntel = ThreatIntelligenceManager.getInstance(context)
                val report = threatIntel.generateAnonymousReport()
                if (report != null) {
                    Log.d(TAG, "Threat intel report: ${report.threatCount} threats, ${report.phoneHashes.size} phones, ${report.domainHashes.size} domains")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Threat intel report generation failed: ${e.message}")
            }

            if (!PermissionManager.hasNotificationPermission(context)) {
                return Result.success()
            }

            val db = DatabaseFactory.getInstance(context)
            val dao = db.fraudDao()
            val since = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)

            val recentSms = dao.getAllSmsList(500).filter { it.timestamp > since }
            val suspiciousSms = recentSms.filter { it.fraudRiskScore > 0.3f }
            val recentSessions = dao.getRecentSessionsList(50).filter { it.createdAt > since }

            if (suspiciousSms.isEmpty() && recentSessions.isEmpty()) {
                showDigestNotification(
                    "All Clear",
                    "No threats detected in the last 24 hours. All channels are secure."
                )
            } else {
                val summary = buildString {
                    append("Last 24h: ")
                    append("${suspiciousSms.size} suspicious SMS")
                    if (recentSessions.isNotEmpty()) {
                        append(", ${recentSessions.size} correlated threats")
                    }
                    val criticalCount = suspiciousSms.count { it.fraudRiskScore > 0.7f }
                    if (criticalCount > 0) {
                        append(" ($criticalCount critical)")
                    }
                }
                showDigestNotification("Daily Security Digest", summary)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Digest generation failed", e)
            Result.retry()
        }
    }

    private fun showDigestNotification(title: String, body: String) {
        RakshakNotificationChannels.bootstrap(applicationContext)
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(applicationContext, RakshakNotificationChannels.DIGEST)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(DIGEST_NOTIFICATION_ID, notification)
    }
}
