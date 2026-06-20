package com.security.rakshakx.core.modelupdate

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.concurrent.TimeUnit

class ModelUpdateManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "ModelUpdate"
        private const val PREFS_NAME = "rakshakx_model_update"
        private const val KEY_MODEL_VERSION = "model_version"
        private const val KEY_RULE_VERSION = "rule_version"
        private const val KEY_AUTO_UPDATE = "auto_update_enabled"
        private const val KEY_LAST_CHECK = "last_update_check"
        private const val WORK_NAME = "model_update_check"

        @Volatile
        private var instance: ModelUpdateManager? = null

        fun getInstance(context: Context): ModelUpdateManager {
            return instance ?: synchronized(this) {
                instance ?: ModelUpdateManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentModelVersion = MutableStateFlow(prefs.getString(KEY_MODEL_VERSION, "2.0.0") ?: "2.0.0")
    val currentModelVersion: StateFlow<String> = _currentModelVersion

    private val _currentRuleVersion = MutableStateFlow(prefs.getInt(KEY_RULE_VERSION, 1))
    val currentRuleVersion: StateFlow<Int> = _currentRuleVersion

    private val _autoUpdateEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_UPDATE, true))
    val autoUpdateEnabled: StateFlow<Boolean> = _autoUpdateEnabled

    fun setAutoUpdate(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_UPDATE, enabled).apply()
        _autoUpdateEnabled.value = enabled
        if (enabled) scheduleUpdateCheck() else cancelUpdateCheck()
    }

    fun getModelDirectory(): File {
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getLastCheckTime(): Long = prefs.getLong(KEY_LAST_CHECK, 0L)

    fun updateModelVersion(version: String) {
        prefs.edit().putString(KEY_MODEL_VERSION, version).apply()
        _currentModelVersion.value = version
        Log.d(TAG, "Model version updated to $version")
    }

    fun updateRuleVersion(version: Int) {
        prefs.edit().putInt(KEY_RULE_VERSION, version).apply()
        _currentRuleVersion.value = version
        Log.d(TAG, "Rule version updated to $version")
    }

    fun rollbackModel(previousVersion: String) {
        val modelDir = getModelDirectory()
        val backupDir = File(modelDir, "backup_$previousVersion")
        if (backupDir.exists()) {
            val currentDir = File(modelDir, "current")
            currentDir.deleteRecursively()
            backupDir.copyRecursively(currentDir, overwrite = true)
            updateModelVersion(previousVersion)
            Log.i(TAG, "Rolled back to model version $previousVersion")
        } else {
            Log.w(TAG, "No backup found for version $previousVersion")
        }
    }

    fun scheduleUpdateCheck() {
        val request = PeriodicWorkRequestBuilder<ModelUpdateWorker>(
            12, TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        Log.d(TAG, "Model update check scheduled")
    }

    fun cancelUpdateCheck() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun recordCheckTime() {
        prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
    }
}

class ModelUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val manager = ModelUpdateManager.getInstance(applicationContext)
        manager.recordCheckTime()

        Log.d("ModelUpdate", "Checking for model updates... (current: ${manager.currentModelVersion.value})")

        // In production: check a manifest endpoint for new model versions
        // Download ONNX model files to getModelDirectory()
        // Validate checksum, swap atomically, update version

        return Result.success()
    }
}
