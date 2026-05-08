package com.security.rakshakx.web.ui

import android.app.Activity
import android.content.Context
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.security.rakshakx.web.services.FraudVpnService
import com.security.rakshakx.web.utils.VpnStatusStore

@Composable
fun VpnDashboardScreen(context: Context, modifier: Modifier = Modifier) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            FraudVpnService.start(context)
        }
    }

    val prepareIntent = remember { VpnService.prepare(context) }
    val isRunning = VpnStatusStore.isRunning.collectAsState().value

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "RakshakX VPN Protection", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Live Protection")
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "VPN Status: ${if (isRunning) "RUNNING" else "STOPPED"}")
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        if (prepareIntent != null) {
                            launcher.launch(prepareIntent)
                        } else {
                            FraudVpnService.start(context)
                        }
                    }) {
                        Text("Start VPN")
                    }

                    Button(onClick = { FraudVpnService.stop(context) }) {
                        Text("Stop VPN")
                    }
                }
            }
        }

        Card(modifier = Modifier.padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Threat Statistics")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Blocked domains: 0")
                Text("High risk alerts: 0")
                Text("Active browser sessions: 0")
            }
        }

        Card(modifier = Modifier.padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Recent Correlation")
                Spacer(modifier = Modifier.height(8.dp))
                Text("No correlated events yet")
            }
        }
    }
}
