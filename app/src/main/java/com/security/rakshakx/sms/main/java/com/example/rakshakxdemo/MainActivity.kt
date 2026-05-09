package com.example.rakshakxdemo

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private var smsGranted        = mutableStateOf(false)
    private var postNotifGranted  = mutableStateOf(false)
    private var listenerGranted   = mutableStateOf(false)

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshState() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Refresh on every recompose so status updates when user comes back
            LaunchedEffect(Unit) { refreshState() }

            val sms      by smsGranted
            val postNotif by postNotifGranted
            val listener by listenerGranted

            val needsPostNotif = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            val allReady = sms && listener && (!needsPostNotif || postNotif)

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF060610)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // ── Header ─────────────────────────────────────────
                        Text("🛡️ RakshakX",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00E5FF))

                        Text("Fraud Interception System",
                            fontSize = 13.sp,
                            color = Color(0xFF555577))

                        Spacer(Modifier.height(8.dp))

                        // ── Status card ────────────────────────────────────
                        StatusCard(
                            sms       = sms,
                            postNotif = if (needsPostNotif) postNotif else true,
                            listener  = listener
                        )

                        // ── Step 1: SMS permission ─────────────────────────
                        if (!sms || (needsPostNotif && !postNotif)) {
                            StepButton(
                                step   = "Step 1",
                                label  = "Grant SMS Permission",
                                color  = Color(0xFF00E5FF),
                                done   = sms && (!needsPostNotif || postNotif)
                            ) { requestRuntimePermissions() }
                        }

                        // ── Step 2: Notification Access ────────────────────
                        // This is the CRITICAL one on Android 15 — without it nothing works
                        if (!listener) {
                            StepButton(
                                step  = "Step 2 — REQUIRED on Android 15",
                                label = "Enable Notification Access →",
                                color = Color(0xFFFF6B35),
                                done  = false
                            ) { openNotificationAccess() }

                            // Clear instructions for the user
                            InstructionCard(
                                text = "In the next screen:\n\n" +
                                        "1. Scroll down to find 'RakshakX'\n" +
                                        "2. Tap it and turn it ON\n" +
                                        "3. Tap 'Allow' on the confirmation dialog\n" +
                                        "4. Press Back to return here\n\n" +
                                        "⚠️  On Android 15 this is the ONLY way " +
                                        "RakshakX can read SMS from Google Messages " +
                                        "or Samsung Messages. Without it, no alerts will fire."
                            )
                        }



                        // ── All good banner ────────────────────────────────
                        if (allReady) {
                            ReadyBanner()
                        }

                        // ── Debug: Test buttons (multilingual fraud alerts) ──
                    }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshState()
        // Start direct SMS inbox polling if READ_SMS is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            == PackageManager.PERMISSION_GRANTED) {
            SmsPollingWorker.schedule(this)
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun refreshState() {
        smsGranted.value = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        postNotifGranted.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        listenerGranted.value = isNotificationListenerEnabled()
    }

    private fun requestRuntimePermissions() {
        val perms = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permLauncher.launch(perms.toTypedArray())
    }

    private fun openNotificationAccess() {
        // ACTION_NOTIFICATION_LISTENER_SETTINGS takes the user to the exact screen
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
        ) ?: return false
        val own = ComponentName(this, NotificationService::class.java)
        return flat.split(":").any { entry ->
            try { ComponentName.unflattenFromString(entry) == own }
            catch (_: Exception) { false }
        }
    }
}

// -----------------------------------------------------------------------
// Composables
// -----------------------------------------------------------------------

@Composable
private fun StatusCard(sms: Boolean, postNotif: Boolean, listener: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF111128))
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Permission Status",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White)

            PermRow("Receive SMS",                       sms)
            PermRow("Show Notifications",                postNotif)
            PermRow("Notification Access (all SMS apps)", listener)
        }
    }
}

@Composable
private fun PermRow(label: String, granted: Boolean) {
    Row(Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFFBBBBCC), fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(
            if (granted) "✅ Granted" else "❌ Missing",
            color = if (granted) Color(0xFF69F0AE) else Color(0xFFFF5252),
            fontSize = 13.sp, fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StepButton(
    step: String,
    label: String,
    color: Color,
    done: Boolean,
    onClick: () -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(step, fontSize = 11.sp, color = Color(0xFF666688))
        Button(
            onClick  = onClick,
            enabled  = !done,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = color),
            shape    = RoundedCornerShape(12.dp)
        ) {
            Text(if (done) "✅  Done" else label,
                color = if (color == Color(0xFF00E5FF)) Color.Black else Color.White,
                fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TestButton(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border   = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InstructionCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF1A1008))
    ) {
        Text(text,
            modifier  = Modifier.padding(16.dp),
            color     = Color(0xFFFFCC80),
            fontSize  = 13.sp,
            lineHeight = 21.sp)
    }
}

@Composable
private fun ReadyBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF081A10))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("✅", fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("RakshakX is fully active",
                    color = Color(0xFF69F0AE), fontWeight = FontWeight.Bold)
                Text("All SMS from every messaging app are now monitored.",
                    color = Color(0xFF4A6050), fontSize = 12.sp)
            }
        }
    }
}