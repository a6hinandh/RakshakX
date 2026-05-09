package com.security.rakshakx

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

import com.security.rakshakx.email.ui.ThreatHistoryScreen
import com.security.rakshakx.call.CallMainActivity
import com.security.rakshakx.ui.theme.RakshakXTheme
import com.security.rakshakx.web.ui.VpnDashboardScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1
            )
        }

        createNotificationChannel()

        setContent {

            RakshakXTheme {

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    Column(

                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),

                        verticalArrangement =
                            Arrangement.Top,

                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        // VPN Dashboard
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {

                            VpnDashboardScreen(
                                activity = this@MainActivity
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        CallMonitoringSection(
                            onOpenCallMonitoring = {
                                startActivity(
                                    Intent(this@MainActivity, CallMainActivity::class.java)
                                )
                            }
                        )

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        // Threat History Dashboard
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {

                            ThreatHistoryScreen(
                                context = this@MainActivity
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        // Email testing section
                        EmailTestingSection(
                            onSendTestNotification = {
                                sendFakePhishingNotification()
                            }
                        )

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )
                    }
                }
            }
        }
    }

    // Notification channel
    private fun createNotificationChannel() {

        val channel = NotificationChannel(
            "rakshakx_channel",
            "RakshakX Alerts",
            NotificationManager.IMPORTANCE_HIGH
        )

        channel.description =
            "Email phishing detection alerts"

        val manager =
            getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        manager.createNotificationChannel(channel)
    }

    // Fake phishing notification
    private fun sendFakePhishingNotification() {

        // Permission check
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            val permissionGranted =

                ContextCompat.checkSelfPermission(

                    this,
                    Manifest.permission.POST_NOTIFICATIONS

                ) == PackageManager.PERMISSION_GRANTED

            if (!permissionGranted) {
                return
            }
        }

        val builder = NotificationCompat.Builder(
            this,
            "rakshakx_channel"
        )

            .setSmallIcon(
                android.R.drawable.ic_dialog_alert
            )

            .setContentTitle(
                "URGENT SECURITY ALERT"
            )

            .setContentText(
                "Verify your BANK account immediately at http://fake-bank.xyz"
            )

            .setPriority(
                NotificationCompat.PRIORITY_HIGH
            )

        NotificationManagerCompat
            .from(this)
            .notify(1001, builder.build())
    }
}

@Composable
fun EmailTestingSection(
    onSendTestNotification: () -> Unit
) {

    Column(

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text = "RakshakX Email Threat Testing",
            style =
                MaterialTheme.typography.headlineSmall
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Button(
            onClick = onSendTestNotification
        ) {

            Text("Trigger Phishing Test")
        }
    }
}

@Composable
fun CallMonitoringSection(
    onOpenCallMonitoring: () -> Unit
) {

    Column(

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text = "RakshakX Call & SMS Monitoring",
            style =
                MaterialTheme.typography.headlineSmall
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Button(
            onClick = onOpenCallMonitoring
        ) {

            Text("Open Call Monitoring")
        }
    }
}