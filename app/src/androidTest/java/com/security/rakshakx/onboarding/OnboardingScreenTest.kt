package com.security.rakshakx.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.security.rakshakx.permissions.PermissionManager
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun continueButton_isDisabled_whenMinimumRequirementsMissing() {
        composeRule.setContent {
            MaterialTheme {
                OnboardingScreen(
                    readinessState = PermissionManager.ReadinessState(
                        corePermissionsGranted = false,
                        notificationListenerEnabled = false,
                        accessibilityEnabled = false,
                        smsReady = false,
                        callReady = false,
                        emailReady = false,
                        webReady = false,
                        minimumDashboardReady = false
                    ),
                    missingRequirements = listOf("Core runtime permissions"),
                    onComplete = {},
                    onRequestCorePermissions = {},
                    onRequestAccessibility = {},
                    onRequestNotificationAccess = {}
                )
            }
        }

        composeRule.onNodeWithText("Complete Required Access First").assertIsNotEnabled()
    }

    @Test
    fun continueButton_isEnabled_whenAllRequirementsReady() {
        composeRule.setContent {
            MaterialTheme {
                OnboardingScreen(
                    readinessState = PermissionManager.ReadinessState(
                        corePermissionsGranted = true,
                        notificationListenerEnabled = true,
                        accessibilityEnabled = true,
                        smsReady = true,
                        callReady = true,
                        emailReady = true,
                        webReady = true,
                        minimumDashboardReady = true
                    ),
                    missingRequirements = emptyList(),
                    onComplete = {},
                    onRequestCorePermissions = {},
                    onRequestAccessibility = {},
                    onRequestNotificationAccess = {}
                )
            }
        }

        composeRule.onNodeWithText("Go to Dashboard").assertIsEnabled()
    }
}

