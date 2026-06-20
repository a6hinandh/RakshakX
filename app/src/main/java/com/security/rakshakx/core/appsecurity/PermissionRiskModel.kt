package com.security.rakshakx.core.appsecurity

// ─────────────────────────────────────────────────────────────────
// PermissionDetail — one risky permission with metadata
// ─────────────────────────────────────────────────────────────────

data class PermissionDetail(
    val permission: String,
    val shortName: String,
    val description: String,
    val riskLevel: String,   // "Critical", "High", "Medium", "Low"
    val weight: Int
)

// ─────────────────────────────────────────────────────────────────
// PermissionRiskModel — static risk database + scoring
// ─────────────────────────────────────────────────────────────────

object PermissionRiskModel {

    // Full Android permission name → (weight, shortName, description, riskLevel)
    private data class PermEntry(
        val weight: Int,
        val shortName: String,
        val description: String,
        val riskLevel: String
    )

    private val PERMISSION_DB: Map<String, PermEntry> = mapOf(
        "android.permission.CAMERA" to PermEntry(15, "Camera", "Access the device camera for photos and video.", "High"),
        "android.permission.RECORD_AUDIO" to PermEntry(15, "Microphone", "Record audio via the microphone.", "High"),
        "android.permission.READ_CONTACTS" to PermEntry(10, "Read Contacts", "Read all contacts stored on the device.", "Medium"),
        "android.permission.WRITE_CONTACTS" to PermEntry(8, "Write Contacts", "Add or modify contacts.", "Medium"),
        "android.permission.ACCESS_FINE_LOCATION" to PermEntry(15, "Precise Location", "Access GPS-level device location.", "High"),
        "android.permission.ACCESS_COARSE_LOCATION" to PermEntry(8, "Approximate Location", "Access cell-tower/Wi-Fi level location.", "Medium"),
        "android.permission.ACCESS_BACKGROUND_LOCATION" to PermEntry(25, "Background Location", "Access location even when the app is not in use.", "Critical"),
        "android.permission.READ_SMS" to PermEntry(20, "Read SMS", "Read all SMS messages on the device.", "Critical"),
        "android.permission.RECEIVE_SMS" to PermEntry(20, "Receive SMS", "Intercept incoming SMS messages before they are delivered.", "Critical"),
        "android.permission.SEND_SMS" to PermEntry(18, "Send SMS", "Send SMS messages (potential toll fraud).", "High"),
        "android.permission.READ_CALL_LOG" to PermEntry(20, "Read Call Log", "Access the full call history of the device.", "Critical"),
        "android.permission.WRITE_CALL_LOG" to PermEntry(15, "Write Call Log", "Modify or delete call history entries.", "High"),
        "android.permission.PROCESS_OUTGOING_CALLS" to PermEntry(20, "Intercept Calls", "Intercept and redirect outgoing phone calls.", "Critical"),
        "android.permission.SYSTEM_ALERT_WINDOW" to PermEntry(25, "Draw Over Apps", "Draw UI overlays on top of other apps (overlay attack vector).", "Critical"),
        "android.permission.BIND_ACCESSIBILITY_SERVICE" to PermEntry(30, "Accessibility Service", "Full control over all UI interactions — used by keyloggers and spyware.", "Critical"),
        "android.permission.WRITE_EXTERNAL_STORAGE" to PermEntry(10, "Write Storage", "Write files to external/shared storage.", "Medium"),
        "android.permission.READ_EXTERNAL_STORAGE" to PermEntry(8, "Read Storage", "Read files from external/shared storage.", "Low"),
        "android.permission.MANAGE_EXTERNAL_STORAGE" to PermEntry(20, "Manage All Files", "Full access to all files on the device.", "Critical"),
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" to PermEntry(25, "Notification Access", "Read all notifications from every app, including OTPs.", "Critical"),
        "android.permission.BIND_DEVICE_ADMIN" to PermEntry(30, "Device Admin", "Enforce device policies, wipe device, lock screen.", "Critical"),
        "android.permission.REQUEST_INSTALL_PACKAGES" to PermEntry(25, "Install Packages", "Silently install APKs — a primary dropper mechanism.", "Critical"),
        "android.permission.CHANGE_NETWORK_STATE" to PermEntry(10, "Change Network", "Enable or disable Wi-Fi, mobile data connections.", "Medium"),
        "android.permission.READ_PHONE_STATE" to PermEntry(12, "Read Phone State", "Access IMEI, SIM serial, phone number, and call state.", "Medium"),
        "android.permission.CALL_PHONE" to PermEntry(15, "Make Calls", "Initiate phone calls without user interaction.", "High"),
        "android.permission.READ_PHONE_NUMBERS" to PermEntry(10, "Read Phone Numbers", "Read the device phone number(s).", "Medium"),
        "android.permission.USE_BIOMETRIC" to PermEntry(5, "Biometric", "Use fingerprint/face for authentication.", "Low"),
        "android.permission.BLUETOOTH_SCAN" to PermEntry(8, "Bluetooth Scan", "Discover nearby Bluetooth devices.", "Low"),
        "android.permission.BLUETOOTH_CONNECT" to PermEntry(10, "Bluetooth Connect", "Connect to paired Bluetooth devices.", "Medium"),
        "android.permission.NFC" to PermEntry(10, "NFC", "Read/write NFC tags and communicate with NFC devices.", "Medium"),
        "android.permission.BODY_SENSORS" to PermEntry(12, "Body Sensors", "Access heart rate and other body sensor data.", "Medium"),
        "android.permission.ACTIVITY_RECOGNITION" to PermEntry(8, "Activity Recognition", "Detect physical activity (walking, driving, etc.).", "Low"),
        "android.permission.GET_ACCOUNTS" to PermEntry(10, "Get Accounts", "Discover all Google/email accounts on the device.", "Medium"),
        "android.permission.USE_CREDENTIALS" to PermEntry(15, "Use Credentials", "Access credentials for accounts on the device.", "High"),
        "android.permission.INTERNET" to PermEntry(5, "Internet", "Access the internet.", "Low"),
        "android.permission.FOREGROUND_SERVICE" to PermEntry(5, "Foreground Service", "Run a persistent foreground service.", "Low"),
        "android.permission.RECEIVE_BOOT_COMPLETED" to PermEntry(8, "Auto-Start", "Start automatically when the device boots.", "Low"),
        "android.permission.WAKE_LOCK" to PermEntry(5, "Wake Lock", "Prevent the processor from sleeping.", "Low"),
        "android.permission.VIBRATE" to PermEntry(2, "Vibrate", "Control the vibration motor.", "Low"),
        "android.permission.ACCESS_WIFI_STATE" to PermEntry(5, "Wi-Fi State", "Read Wi-Fi connection details including SSID.", "Low"),
        "android.permission.CHANGE_WIFI_STATE" to PermEntry(8, "Change Wi-Fi", "Enable, disable, or reconfigure Wi-Fi.", "Low"),
    )

    // ── Combination risk bonuses ──────────────────────────────────

    private data class CombinationBonus(
        val permissions: Set<String>,
        val bonus: Int,
        val reason: String
    )

    private val COMBINATION_BONUSES = listOf(
        CombinationBonus(
            permissions = setOf(
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.INTERNET"
            ),
            bonus = 20,
            reason = "CAMERA + MICROPHONE + INTERNET: potential spyware combination"
        ),
        CombinationBonus(
            permissions = setOf(
                "android.permission.READ_CONTACTS",
                "android.permission.READ_SMS",
                "android.permission.INTERNET"
            ),
            bonus = 15,
            reason = "CONTACTS + SMS + INTERNET: data harvesting combination"
        ),
        CombinationBonus(
            permissions = setOf(
                "android.permission.SYSTEM_ALERT_WINDOW",
                "android.permission.BIND_ACCESSIBILITY_SERVICE"
            ),
            bonus = 20,
            reason = "OVERLAY + ACCESSIBILITY: overlay attack / credential theft combination"
        ),
        CombinationBonus(
            permissions = setOf(
                "android.permission.RECEIVE_SMS",
                "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
                "android.permission.INTERNET"
            ),
            bonus = 15,
            reason = "SMS + NOTIFICATIONS + INTERNET: OTP interception combination"
        ),
        CombinationBonus(
            permissions = setOf(
                "android.permission.REQUEST_INSTALL_PACKAGES",
                "android.permission.INTERNET"
            ),
            bonus = 10,
            reason = "INSTALL_PACKAGES + INTERNET: remote dropper combination"
        )
    )

    // ── Public API ────────────────────────────────────────────────

    /**
     * Compute an aggregate risk score (0-100) for a list of permissions.
     * Includes combination bonuses for dangerous permission clusters.
     */
    fun computeRisk(permissions: List<String>): Int {
        val permSet = permissions.toSet()

        // Base sum of individual weights
        var total = permSet.sumOf { perm -> PERMISSION_DB[perm]?.weight ?: 0 }

        // Apply combination bonuses
        for (combo in COMBINATION_BONUSES) {
            if (permSet.containsAll(combo.permissions)) {
                total += combo.bonus
            }
        }

        return total.coerceIn(0, 100)
    }

    /**
     * Return only the permissions that appear in the risk database,
     * enriched with metadata.
     */
    fun getDangerousPermissions(permissions: List<String>): List<PermissionDetail> =
        permissions.mapNotNull { perm ->
            PERMISSION_DB[perm]?.let { entry ->
                PermissionDetail(
                    permission = perm,
                    shortName  = entry.shortName,
                    description = entry.description,
                    riskLevel  = entry.riskLevel,
                    weight     = entry.weight
                )
            }
        }.sortedByDescending { it.weight }

    /** Lookup the raw weight for a single permission (0 if unknown). */
    fun weightOf(permission: String): Int = PERMISSION_DB[permission]?.weight ?: 0
}
