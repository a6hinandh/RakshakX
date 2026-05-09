package com.security.rakshakx
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

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

                androidx.compose.material3.Surface {

                    androidx.compose.foundation.layout.Column {

                        androidx.compose.material3.Button(

                            onClick = {

                                com.security.rakshakx.email.pipeline.EmailThreatPipeline.process(

                                    context = this@MainActivity,

                                    title = "URGENT BANK ALERT",

                                    body =
                                        "Verify OTP immediately or your bank account will be blocked. Click https://fake-bank.xyz now!",

                                    persistenceScope = null,

                                    logPrefix = "MANUAL EMAIL TEST"
                                )
                            }

                        ) {

                            androidx.compose.material3.Text(
                                "Trigger Email Phishing Test"
                            )
                        }
                    }
                }
            }
        }
    }
}
