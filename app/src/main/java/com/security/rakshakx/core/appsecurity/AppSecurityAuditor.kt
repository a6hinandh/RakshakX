package com.security.rakshakx.core.appsecurity

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────
// Enums
// ─────────────────────────────────────────────────────────────────

enum class InstallSource { PLAY_STORE, SIDELOADED, SYSTEM, UNKNOWN }

enum class AppRiskLevel(val label: String) {
    SAFE("Safe"),
    LOW("Low Risk"),
    MEDIUM("Medium"),
    HIGH("High Risk"),
    CRITICAL("Critical")
}

// ─────────────────────────────────────────────────────────────────
// AppRiskProfile — per-app result
// ─────────────────────────────────────────────────────────────────

data class AppRiskProfile(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val installSource: InstallSource,
    val requestedPermissions: List<String>,
    val dangerousPermissions: List<PermissionDetail>,
    val riskScore: Int,
    val riskLevel: AppRiskLevel,
    val riskReasons: List<String>,
    val isSystemApp: Boolean
)

// ─────────────────────────────────────────────────────────────────
// AppSecurityAuditor — singleton
// ─────────────────────────────────────────────────────────────────

class AppSecurityAuditor private constructor(private val context: Context) {

    companion object {
        @Volatile private var instance: AppSecurityAuditor? = null

        fun getInstance(context: Context): AppSecurityAuditor =
            instance ?: synchronized(this) {
                instance ?: AppSecurityAuditor(context.applicationContext).also { instance = it }
            }

        private const val PLAY_STORE_PACKAGE = "com.android.vending"
    }

    // ── Public API ────────────────────────────────────────────────

    suspend fun auditInstalledApps(): List<AppRiskProfile> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PackageManager.GET_PERMISSIONS or PackageManager.MATCH_UNINSTALLED_PACKAGES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_PERMISSIONS
        }

        val packages: List<PackageInfo> = try {
            pm.getInstalledPackages(flags)
        } catch (_: Exception) {
            emptyList()
        }

        packages
            .filter { pkg ->
                // Exclude pure system apps (no user-visible launcher icon / not updated)
                val isSystem = (pkg.applicationInfo?.flags
                    ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
                val isUpdatedSystem = (pkg.applicationInfo?.flags
                    ?: 0) and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
                !isSystem || isUpdatedSystem   // keep user apps + updated system apps
            }
            .mapNotNull { pkg -> buildProfile(pm, pkg) }
            .sortedByDescending { it.riskScore }
    }

    suspend fun auditSingleApp(packageName: String): AppRiskProfile? = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        return@withContext try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PackageManager.GET_PERMISSIONS or PackageManager.MATCH_UNINSTALLED_PACKAGES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_PERMISSIONS
            }
            val pkg = pm.getPackageInfo(packageName, flags)
            buildProfile(pm, pkg)
        } catch (_: Exception) {
            null
        }
    }

    // ── Internal ──────────────────────────────────────────────────

    private fun buildProfile(pm: PackageManager, pkg: PackageInfo): AppRiskProfile? {
        val appInfo = pkg.applicationInfo ?: return null

        val appName = try {
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            pkg.packageName
        }

        val versionName = pkg.versionName ?: "unknown"

        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

        // ── Install source ────────────────────────────────────────
        val installSource = detectInstallSource(pm, pkg.packageName, isSystemApp)

        // ── Permissions ───────────────────────────────────────────
        val allPermissions = pkg.requestedPermissions?.toList() ?: emptyList()
        val dangerousPerms = PermissionRiskModel.getDangerousPermissions(allPermissions)

        // ── Risk score ────────────────────────────────────────────
        val reasons = mutableListOf<String>()
        var score = PermissionRiskModel.computeRisk(allPermissions)

        if (installSource == InstallSource.SIDELOADED) {
            score += 20
            reasons += "Sideloaded (not from Play Store)"
        }

        if (dangerousPerms.size > 10) {
            score += 10
            reasons += "Requests more than 10 high-risk permissions"
        }

        val accessibilityPerm = "android.permission.BIND_ACCESSIBILITY_SERVICE"
        val notificationPerm  = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
        val deviceAdminPerm   = "android.permission.BIND_DEVICE_ADMIN"

        if (allPermissions.contains(accessibilityPerm)) {
            reasons += "Uses Accessibility Service — can read all screen content"
        }
        if (allPermissions.contains(notificationPerm)) {
            reasons += "Notification Listener access — can read OTPs and private messages"
        }
        if (allPermissions.contains(deviceAdminPerm)) {
            reasons += "Device Administrator — can remotely lock or wipe the device"
            score += 10
        }

        score = score.coerceIn(0, 100)

        val riskLevel = when {
            score >= 80 -> AppRiskLevel.CRITICAL
            score >= 60 -> AppRiskLevel.HIGH
            score >= 40 -> AppRiskLevel.MEDIUM
            score >= 15 -> AppRiskLevel.LOW
            else        -> AppRiskLevel.SAFE
        }

        return AppRiskProfile(
            packageName          = pkg.packageName,
            appName              = appName,
            versionName          = versionName,
            installSource        = installSource,
            requestedPermissions = allPermissions,
            dangerousPermissions = dangerousPerms,
            riskScore            = score,
            riskLevel            = riskLevel,
            riskReasons          = reasons,
            isSystemApp          = isSystemApp
        )
    }

    private fun detectInstallSource(
        pm: PackageManager,
        packageName: String,
        isSystemApp: Boolean
    ): InstallSource {
        if (isSystemApp) return InstallSource.SYSTEM
        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageName)
            }
            when (installer) {
                PLAY_STORE_PACKAGE -> InstallSource.PLAY_STORE
                null               -> InstallSource.SIDELOADED
                else               -> InstallSource.UNKNOWN
            }
        } catch (_: Exception) {
            InstallSource.UNKNOWN
        }
    }
}
