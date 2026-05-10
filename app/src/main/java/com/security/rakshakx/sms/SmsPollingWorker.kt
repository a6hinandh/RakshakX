package com.security.rakshakx.sms

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * SmsPollingWorker — queries the Android SMS content provider directly.
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

        private val SMS_URI = Uri.parse("content://sms/inbox")

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SmsPollingWorker>(
                15, TimeUnit.SECONDS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "SMS polling scheduled")
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
        if (!hasSmsPermission()) return Result.success()
        return try {
            pollSmsInbox()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error polling inbox", e)
            Result.retry()
        }
    }

    private fun pollSmsInbox() {
        var lastTimestamp = prefs.getLong(KEY_LAST_SMS_TS, 0L)

        // First run: skip history
        if (lastTimestamp == 0L) {
            lastTimestamp = System.currentTimeMillis()
            prefs.edit().putLong(KEY_LAST_SMS_TS, lastTimestamp).apply()
            return
        }

        var newestTimestamp = lastTimestamp
        val projection = arrayOf("address", "body", "date")
        val selection = "date > ?"
        val selectionArgs = arrayOf(lastTimestamp.toString())

        context.contentResolver.query(
            SMS_URI, projection, selection, selectionArgs, "date DESC"
        )?.use { cursor ->
            val addressCol = cursor.getColumnIndexOrThrow("address")
            val bodyCol    = cursor.getColumnIndexOrThrow("body")
            val dateCol    = cursor.getColumnIndexOrThrow("date")

            val detector = SmsScamDetector(context)

            while (cursor.moveToNext()) {
                val sender    = cursor.getString(addressCol) ?: "Unknown"
                val body      = cursor.getString(bodyCol)    ?: ""
                val timestamp = cursor.getLong(dateCol)

                if (!SmsDeduplicationGuard.shouldProcess(context, sender, body)) continue

                if (timestamp > newestTimestamp) newestTimestamp = timestamp

                // Use the Hybrid ML Pipeline
                detector.analyze(sender, body)
            }
        }

        if (newestTimestamp > lastTimestamp) {
            prefs.edit().putLong(KEY_LAST_SMS_TS, newestTimestamp).apply()
        }
    }

    private fun hasSmsPermission() =
        context.checkSelfPermission(android.Manifest.permission.READ_SMS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
}
