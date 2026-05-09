package com.security.rakshakx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.security.rakshakx.notifications.RakshakNotificationChannels
import com.security.rakshakx.startup.AppStartupCoordinator
import com.security.rakshakx.ui.navigation.RakshakXNavHost
import com.security.rakshakx.ui.theme.RakshakXTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        RakshakNotificationChannels.bootstrap(applicationContext)
        AppStartupCoordinator.reconcileOnAppLaunch(applicationContext)

        setContent {
            RakshakXTheme {
                RakshakXNavHost(activity = this)
            }
        }
    }
}
