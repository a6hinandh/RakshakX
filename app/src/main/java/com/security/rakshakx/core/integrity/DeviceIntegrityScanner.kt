package com.security.rakshakx.core.integrity

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────────
// Severity & Finding models
// ─────────────────────────────────────────────────────────────────

enum class FindingSeverity { CRITICAL, HIGH, MEDIUM, LOW, INFO }

data class SecurityFinding(
    val severity: FindingSeverity,
    val title: String,
    val description: String,
    val recommendation: String
)

// ─────────────────────────────────────────────────────────────────
// Result model
// ─────────────────────────────────────────────────────────────────

data class DeviceIntegrityResult(
    val isRooted: Boolean,
    val rootMethod: String?,
    val isDebugBuild: Boolean,
    val adbEnabled: Boolean,
    val devOptionsEnabled: Boolean,
    val unknownSourcesEnabled: Boolean,
    val securityPatchAge: Int,
    val securityPatchDate: String,
    val encryptionStatus: String,
    val screenLockEnabled: Boolean,
    val googlePlayProtectStatus: String,
    val overallScore: Int,
    val findings: List<SecurityFinding>
)

// ─────────────────────────────────────────────────────────────────
// Scanner singleton
// ─────────────────────────────────────────────────────────────────

class DeviceIntegrityScanner private constructor(private val context: Context) {

    companion object {
        @Volatile private var instance: DeviceIntegrityScanner? = null

        fun getInstance(context: Context): DeviceIntegrityScanner =
            instance ?: synchronized(this) {
                instance ?: DeviceIntegrityScanner(context.applicationContext).also { instance = it }
            }
    }

    suspend fun scan(): DeviceIntegrityResult = withContext(Dispatchers.IO) {
        val findings = mutableListOf<SecurityFinding>()

        // ── Root detection ───────────────────────────────────────
        val (isRooted, rootMethod) = detectRoot()
        if (isRooted) {
            findings += SecurityFinding(
                severity = FindingSeverity.CRITICAL,
                title = "Device is Rooted",
                description = rootMethod ?: "Root access detected on this device.",
                recommendation = "Rooting removes OS security guarantees. Avoid banking or sensitive apps on rooted devices."
            )
        }

        // ── Debug build ──────────────────────────────────────────
        val isDebug = Build.TYPE == "userdebug" || Build.TYPE == "eng"
        if (isDebug) {
            findings += SecurityFinding(
                severity = FindingSeverity.HIGH,
                title = "Debug Build Detected",
                description = "This device is running a debug/engineering OS build (Build.TYPE=${Build.TYPE}).",
                recommendation = "Use a production release build for maximum security."
            )
        }

        // ── ADB enabled ──────────────────────────────────────────
        val adbEnabled = Settings.Global.getInt(
            context.contentResolver, Settings.Global.ADB_ENABLED, 0
        ) != 0
        if (adbEnabled) {
            findings += SecurityFinding(
                severity = FindingSeverity.HIGH,
                title = "USB Debugging (ADB) Enabled",
                description = "ADB allows full shell access over USB and sometimes over Wi-Fi.",
                recommendation = "Disable USB debugging in Developer Options when not actively developing."
            )
        }

        // ── Developer options ────────────────────────────────────
        val devOptionsEnabled = Settings.Global.getInt(
            context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
        ) != 0
        if (devOptionsEnabled) {
            findings += SecurityFinding(
                severity = FindingSeverity.MEDIUM,
                title = "Developer Options Enabled",
                description = "Developer Options expose low-level system controls that can be exploited.",
                recommendation = "Disable Developer Options in System Settings if not needed."
            )
        }

        // ── Unknown sources ──────────────────────────────────────
        val unknownSources = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else {
                @Suppress("DEPRECATION")
                Settings.Secure.getInt(
                    context.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS, 0
                ) != 0
            }
        } catch (_: Exception) {
            false
        }
        if (unknownSources) {
            findings += SecurityFinding(
                severity = FindingSeverity.HIGH,
                title = "Unknown Sources Allowed",
                description = "Apps from outside the Play Store can be installed, increasing malware risk.",
                recommendation = "Revoke 'Install unknown apps' permission from all apps that don't require it."
            )
        }

        // ── Security patch age ───────────────────────────────────
        val patchDate = Build.VERSION.SECURITY_PATCH  // "YYYY-MM-DD"
        val patchAge = computePatchAgeDays(patchDate)
        if (patchAge > 90) {
            findings += SecurityFinding(
                severity = if (patchAge > 180) FindingSeverity.HIGH else FindingSeverity.MEDIUM,
                title = "Security Patch Outdated",
                description = "Last patch: $patchDate ($patchAge days ago). Unpatched vulnerabilities may be exploitable.",
                recommendation = "Install available system updates to get the latest security patches."
            )
        }

        // ── Screen lock ──────────────────────────────────────────
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE)
                as? android.app.KeyguardManager
        val screenLockEnabled = keyguardManager?.isDeviceSecure ?: false
        if (!screenLockEnabled) {
            findings += SecurityFinding(
                severity = FindingSeverity.HIGH,
                title = "No Screen Lock Set",
                description = "Device has no PIN, pattern, password, or biometric lock.",
                recommendation = "Enable a strong screen lock (PIN/password) in Security settings."
            )
        }

        // ── Encryption ───────────────────────────────────────────
        val encryptionStatus = detectEncryptionStatus()
        if (encryptionStatus == "Not Encrypted") {
            findings += SecurityFinding(
                severity = FindingSeverity.CRITICAL,
                title = "Device Not Encrypted",
                description = "Data stored on this device is not encrypted and can be read if physically accessed.",
                recommendation = "Enable full-disk or file-based encryption in Security settings."
            )
        }

        // ── Play Protect ─────────────────────────────────────────
        val playProtectStatus = detectPlayProtectStatus()
        if (playProtectStatus == "Disabled") {
            findings += SecurityFinding(
                severity = FindingSeverity.HIGH,
                title = "Google Play Protect Disabled",
                description = "Play Protect scans apps for malware. Disabling it leaves malicious apps undetected.",
                recommendation = "Re-enable Play Protect in the Google Play Store > Play Protect menu."
            )
        }

        // ── Score computation ────────────────────────────────────
        var score = 100
        if (isRooted)           score -= 40
        if (isDebug)            score -= 10
        if (adbEnabled)         score -= 10
        if (devOptionsEnabled)  score -= 5
        if (unknownSources)     score -= 10
        if (patchAge > 90)      score -= 15
        if (!screenLockEnabled) score -= 15
        if (encryptionStatus == "Not Encrypted") score -= 10
        if (playProtectStatus == "Disabled")     score -= 10
        score = score.coerceIn(0, 100)

        DeviceIntegrityResult(
            isRooted = isRooted,
            rootMethod = rootMethod,
            isDebugBuild = isDebug,
            adbEnabled = adbEnabled,
            devOptionsEnabled = devOptionsEnabled,
            unknownSourcesEnabled = unknownSources,
            securityPatchAge = patchAge,
            securityPatchDate = patchDate,
            encryptionStatus = encryptionStatus,
            screenLockEnabled = screenLockEnabled,
            googlePlayProtectStatus = playProtectStatus,
            overallScore = score,
            findings = findings
        )
    }

    // ─────────────────────────────────────────────────────────────
    // Root detection — multi-method
    // ─────────────────────────────────────────────────────────────

    private fun detectRoot(): Pair<Boolean, String?> {
        // 1. su binary paths
        val suPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/data/local/su",
            "/data/local/bin/su"
        )
        for (path in suPaths) {
            if (File(path).exists()) return true to "su binary found at $path"
        }

        // 2. test-keys build tag
        val buildTags = Build.TAGS ?: ""
        if (buildTags.contains("test-keys")) {
            return true to "Build signed with test-keys (indicates unofficial/rooted build)"
        }

        // 3. Magisk paths
        val magiskPaths = listOf("/sbin/.magisk", "/dev/.magisk", "/.magisk")
        for (path in magiskPaths) {
            if (File(path).exists()) return true to "Magisk detected at $path"
        }

        // 4. SuperSU artifacts
        val superSuPaths = listOf(
            "/system/app/Superuser.apk",
            "/system/etc/init.d/99SuperSUDaemon"
        )
        for (path in superSuPaths) {
            if (File(path).exists()) return true to "SuperSU artifact found at $path"
        }

        // 5. Execute "which su"
        try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val result = process.inputStream.bufferedReader().readLine()
            process.destroy()
            if (!result.isNullOrBlank()) return true to "su found via 'which su': $result"
        } catch (_: Exception) { /* no-op */ }

        return false to null
    }

    // ─────────────────────────────────────────────────────────────
    // Encryption status
    // ─────────────────────────────────────────────────────────────

    private fun detectEncryptionStatus(): String {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            when (dpm?.storageEncryptionStatus) {
                DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE,
                DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER,
                DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY -> "Encrypted"
                DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE -> "Not Encrypted"
                else -> "Unknown"
            }
        } catch (_: Exception) {
            "Unknown"
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Play Protect status (heuristic — no direct API)
    // ─────────────────────────────────────────────────────────────

    private fun detectPlayProtectStatus(): String {
        return try {
            // Play Protect setting is stored as a secure setting on many devices
            val value = Settings.Secure.getInt(
                context.contentResolver, "package_verifier_enable", -1
            )
            when (value) {
                1    -> "Active"
                0    -> "Disabled"
                else -> "Unknown"
            }
        } catch (_: Exception) {
            "Unknown"
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Patch age helper
    // ─────────────────────────────────────────────────────────────

    private fun computePatchAgeDays(patchDateStr: String): Int {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val patchDate = sdf.parse(patchDateStr) ?: return 0
            val diffMs = Date().time - patchDate.time
            TimeUnit.MILLISECONDS.toDays(diffMs).toInt().coerceAtLeast(0)
        } catch (_: Exception) {
            0
        }
    }
}
