package com.security.rakshakx.call.services.foreground

import android.app.NotificationManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.security.rakshakx.call.ai.inference.FraudInferenceEngine
import com.security.rakshakx.call.ai.loader.ModelLoader
import com.security.rakshakx.call.core.orchestrator.Orchestrator
import com.security.rakshakx.call.core.orchestrator.RakshakOrchestrator
import com.security.rakshakx.call.core.storage.DatabaseFactory
import com.security.rakshakx.call.core.storage.RiskScoreRepository
import com.security.rakshakx.call.CallMainActivity
import com.security.rakshakx.R
import com.security.rakshakx.notifications.RakshakNotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FraudMonitoringForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannels(this)
        startForeground(
            MONITORING_NOTIFICATION_ID,
            buildMonitoringNotification()
        )
        serviceScope.launch {
            runCatching { getOrchestrator(applicationContext).start() }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(MONITORING_NOTIFICATION_ID, buildMonitoringNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.launch {
            runCatching { getOrchestrator(applicationContext).stop() }
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildMonitoringNotification(): Notification {
        val tapIntent = Intent(this, CallMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            3001,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, MONITORING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("RakshakX is actively monitoring for fraud")
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        private val MONITORING_CHANNEL_ID = RakshakNotificationChannels.STATUS
        val ALERTS_CHANNEL_ID = RakshakNotificationChannels.ALERTS
        private const val MONITORING_NOTIFICATION_ID = 1001

        @Volatile
        private var orchestrator: Orchestrator? = null

        fun getOrchestrator(context: Context): Orchestrator {
            return orchestrator ?: synchronized(this) {
                orchestrator ?: run {
                    val db = DatabaseFactory.getInstance(context.applicationContext)
                    val modelLoader = ModelLoader(context.applicationContext)
                    val fraudInferenceEngine = FraudInferenceEngine(modelLoader)
                    val repository = RiskScoreRepository(
                        riskScoreDao = db.riskScoreDao(),
                        fraudInferenceEngine = fraudInferenceEngine
                    )
                    RakshakOrchestrator(
                        repository = repository,
                        fraudInferenceEngine = fraudInferenceEngine
                    ).also { orchestrator = it }
                }
            }
        }

        fun start(context: Context) {
            val intent = Intent(context, FraudMonitoringForegroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, FraudMonitoringForegroundService::class.java)
            context.stopService(intent)
        }

        fun ensureNotificationChannels(context: Context) {
            RakshakNotificationChannels.bootstrap(context.applicationContext)
        }
    }
}


