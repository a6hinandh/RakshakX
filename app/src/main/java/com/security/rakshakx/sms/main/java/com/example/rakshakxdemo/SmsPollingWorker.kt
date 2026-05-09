package com.example.rakshakxdemo

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * SmsPollingWorker — queries the Android SMS content provider directly.
 *
 * This reads from content://sms/inbox the same database that Google Messages,
 * Samsung Messages, and every other SMS app reads from. It does NOT depend on
 * notifications at all — it goes straight to the source.
 *
 * WHY THIS WORKS ON ANDROID 16:
 *   - READ_SMS permission is already granted
 *   - content://sms is a system content provider accessible to any app
 *     with READ_SMS — no need to be the default SMS app
 *   - WorkManager schedules it to run every 15 seconds reliably
 *
 * HOW IT AVOIDS RE-PROCESSING OLD SMS:
 *   - Stores the timestamp of the last processed SMS in SharedPreferences
 *   - On each run, only fetches SMS newer than that timestamp
 */
class SmsPollingWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        private const val TAG             = "RakshakX_POLL"
        private const val PREFS_NAME      = "rakshak_prefs"
        private const val KEY_LAST_SMS_TS = "last_sms_timestamp"
        private const val WORK_NAME       = "rakshak_sms_poll"

        // SMS content provider URI — this is the raw Android SMS database
        private val SMS_URI = Uri.parse("content://sms/inbox")

        /**
         * Call this from MainActivity to start the repeating poll.
         * Using KEEP policy so we don't schedule duplicates on every app launch.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SmsPollingWorker>(
                15, TimeUnit.SECONDS       // minimum interval WorkManager allows
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)   // run even on low battery
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,           // don't restart if already running
                request
            )

            Log.d(TAG, "SMS polling scheduled every 15 seconds")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "SMS polling cancelled")
        }
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun doWork(): Result {
        Log.d(TAG, "Polling SMS inbox...")

        // Check READ_SMS permission before querying
        if (!hasSmsPermission()) {
            Log.w(TAG, "READ_SMS permission not granted — skipping poll")
            return Result.success()
        }

        return try {
            pollSmsInbox()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error polling SMS inbox", e)
            Result.retry()
        }
    }

    private fun pollSmsInbox() {
        val lastTimestamp = prefs.getLong(KEY_LAST_SMS_TS, 0L)
        var newestTimestamp = lastTimestamp

        Log.d(TAG, "Fetching SMS newer than timestamp: $lastTimestamp")

        // Query columns we need
        val projection = arrayOf(
            "_id",
            "address",      // sender phone number or name
            "body",         // full SMS text — not truncated like notifications
            "date"          // timestamp in ms
        )

        // Only fetch SMS newer than what we've already processed
        val selection = "date > ?"
        val selectionArgs = arrayOf(lastTimestamp.toString())

        // Newest first so we process in order
        val sortOrder = "date DESC"

        context.contentResolver.query(
            SMS_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->

            val idCol      = cursor.getColumnIndexOrThrow("_id")
            val addressCol = cursor.getColumnIndexOrThrow("address")
            val bodyCol    = cursor.getColumnIndexOrThrow("body")
            val dateCol    = cursor.getColumnIndexOrThrow("date")

            Log.d(TAG, "Found ${cursor.count} new SMS messages")

            while (cursor.moveToNext()) {
                val id        = cursor.getLong(idCol)
                val sender    = cursor.getString(addressCol) ?: "Unknown"
                val body      = cursor.getString(bodyCol)    ?: ""
                val timestamp = cursor.getLong(dateCol)

                // Track the newest timestamp we've seen
                if (timestamp > newestTimestamp) {
                    newestTimestamp = timestamp
                }

                Log.d(TAG, "SMS id=$id from=$sender ts=$timestamp body='$body'")

                val risk = RiskEngine.calculate(body, context)
                Log.d(TAG, "Risk score: $risk for SMS from $sender")

                if (risk >= RiskEngine.ALERT_THRESHOLD) {
                    NotificationHelper.showFraudAlert(
                        context   = context,
                        sender    = sender,
                        message   = body,           // FULL body — not truncated
                        riskScore = risk,
                        source    = "SMS Inbox"     // shown in alert so team knows which path fired
                    )
                }
            }
        }

        // Save the newest timestamp so next poll skips these messages
        if (newestTimestamp > lastTimestamp) {
            prefs.edit().putLong(KEY_LAST_SMS_TS, newestTimestamp).apply()
            Log.d(TAG, "Updated last SMS timestamp to: $newestTimestamp")
        }
    }

    private fun hasSmsPermission(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.READ_SMS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}