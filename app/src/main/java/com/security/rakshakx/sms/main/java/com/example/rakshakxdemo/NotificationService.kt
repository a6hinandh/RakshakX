package com.example.rakshakxdemo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * NotificationService — PRIMARY fraud detection engine on Android 10+/15.
 *
 * On Android 15 the raw SMS_RECEIVED broadcast is restricted to the default
 * SMS app only, so this NotificationListenerService is the reliable hook.
 * It reads the notification text posted by whatever SMS app is installed.
 *
 * IMPORTANT: The user must enable this in
 *   Settings → Apps → Special app access → Notification access → RakshakX
 * MainActivity guides them there automatically.
 *
 * Android 14/15 change: NLS can be killed by the system. We promote it to
 * a foreground service with a persistent (but silent) status-bar notification
 * so Android treats it as an active service and doesn't kill it.
 */
class NotificationService : NotificationListenerService() {

    companion object {
        private const val TAG = "RakshakX_NLS"
        private const val FG_CHANNEL_ID   = "rakshak_fg"
        private const val FG_NOTIF_ID     = 1
        private const val ALERT_CHANNEL_ID = "rakshak_alerts"

        /**
         * Every known SMS/MMS app package.
         * Add more here if your testers use other apps.
         * We whitelist so we don't accidentally scan unrelated app notifications.
         */
        val SMS_PACKAGES = setOf(
            // Google
            "com.google.android.apps.messaging",
            // Samsung
            "com.samsung.android.messaging",
            // AOSP / stock Android
            "com.android.mms",
            "com.android.messaging",
            // Third-party popular
            "com.textra",
            "com.moez.QKSMS",
            "com.handcent.nextsms",
            "com.klinker.android.evolve_sms",
            // Carriers
            "com.verizon.messaging.vzmsgs",
            "com.att.messages",
            "com.t_mobile.messages",
            // Signal (when used as SMS app)
            "org.thoughtcrime.securesms",
            // Xiaomi / MIUI
            "com.xiaomi.mipush.sdk",
            "com.android.mms.ui",
        )

        private const val OWN_PACKAGE = "com.example.rakshakxdemo"
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NotificationService created")
        createChannels()
        // Promote to foreground so Android 14/15 won't kill us silently
        startForeground(FG_NOTIF_ID, buildPersistentNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.w(TAG, "NotificationService destroyed — will be rebound by system")
        // The system automatically re-binds a NotificationListenerService
        // when it is destroyed, as long as it remains enabled in settings.
    }

    /**
     * onBind is called when the system binds to this NLS.
     * We override to log it so you can confirm in Logcat that binding happened.
     */
    override fun onBind(intent: android.content.Intent?): IBinder? {
        Log.d(TAG, "onBind called — NLS is now connected")
        return super.onBind(intent)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "onListenerConnected — actively receiving notifications")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "onListenerDisconnected — requesting rebind")
        // Ask the system to rebind us immediately
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(
                android.content.ComponentName(this, NotificationService::class.java)
            )
        }
    }

    // -----------------------------------------------------------------------
    // Core logic
    // -----------------------------------------------------------------------

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return

        // Never scan our own notifications — infinite loop guard
        if (pkg == OWN_PACKAGE) return

        // Only process SMS app notifications
        if (pkg !in SMS_PACKAGES) return

        Log.d(TAG, "SMS app notification from: $pkg")

        val extras = sbn.notification.extras ?: return

        val title   = extras.getString(Notification.EXTRA_TITLE)               ?: ""
        val text    = extras.getCharSequence(Notification.EXTRA_TEXT)
            ?.toString()                                         ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?.toString()                                         ?: ""

        // bigText has the full message body; TEXT is truncated to one line
        val body = bigText.ifBlank { text }

        if (body.isBlank()) {
            Log.d(TAG, "Empty body — skipping")
            return
        }

        val risk = RiskEngine.calculate(body, this)
        Log.d(TAG, "pkg=$pkg sender='$title' risk=$risk body='$body'")

        if (risk >= RiskEngine.ALERT_THRESHOLD) {
            NotificationHelper.showFraudAlert(
                context   = this,
                sender    = title,
                message   = body,
                riskScore = risk,
                source    = "SMS"
            )
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) { /* no-op */ }

    // -----------------------------------------------------------------------
    // Foreground service notification (keeps us alive on Android 14/15)
    // -----------------------------------------------------------------------

    private fun buildPersistentNotification(): Notification {
        return NotificationCompat.Builder(this, FG_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("RakshakX Active")
            .setContentText("Monitoring SMS for fraud")
            .setPriority(NotificationCompat.PRIORITY_MIN)   // silent, collapsed
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Silent foreground-service channel
        if (manager.getNotificationChannel(FG_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    FG_CHANNEL_ID,
                    "RakshakX Status",
                    NotificationManager.IMPORTANCE_MIN          // no sound, no pop-up
                ).apply { setShowBadge(false) }
            )
        }

        // Loud alert channel
        if (manager.getNotificationChannel(ALERT_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    ALERT_CHANNEL_ID,
                    "Rakshak Fraud Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Fires when a suspicious SMS is detected"
                    enableVibration(true)
                }
            )
        }
    }
}