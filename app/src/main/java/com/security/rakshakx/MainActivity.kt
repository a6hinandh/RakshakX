package com.security.rakshakx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.security.rakshakx.ui.theme.RakshakXTheme
import com.security.rakshakx.web.ui.VpnDashboardScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RakshakXTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    VpnDashboardScreen(activity = this@MainActivity, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}