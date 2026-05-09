package com.security.rakshakx.permissions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionManagerTest {

    @Test
    fun readinessState_isFullyReady_whenAllRequirementsAreGranted() {
        val state = PermissionManager.buildReadinessState(
            corePermissionsGranted = true,
            notificationListenerEnabled = true,
            accessibilityEnabled = true,
            receiveSmsGranted = true,
            readSmsGranted = true,
            readCallLogGranted = true,
            readPhoneStateGranted = true,
            recordAudioGranted = true,
            postNotificationsGranted = true
        )

        assertTrue(state.minimumDashboardReady)
        assertTrue(state.smsReady)
        assertTrue(state.callReady)
        assertTrue(state.emailReady)
        assertTrue(state.webReady)
    }

    @Test
    fun readinessState_blocksDashboard_whenNotificationOrAccessibilityMissing() {
        val state = PermissionManager.buildReadinessState(
            corePermissionsGranted = true,
            notificationListenerEnabled = false,
            accessibilityEnabled = true,
            receiveSmsGranted = true,
            readSmsGranted = true,
            readCallLogGranted = true,
            readPhoneStateGranted = true,
            recordAudioGranted = true,
            postNotificationsGranted = true
        )

        assertFalse(state.minimumDashboardReady)
        assertFalse(state.smsReady)
        assertFalse(state.emailReady)
        assertTrue(state.callReady)
        assertTrue(state.webReady)
    }

    @Test
    fun readinessState_marksCallNotReady_whenPhonePermissionsMissing() {
        val state = PermissionManager.buildReadinessState(
            corePermissionsGranted = false,
            notificationListenerEnabled = true,
            accessibilityEnabled = true,
            receiveSmsGranted = true,
            readSmsGranted = true,
            readCallLogGranted = true,
            readPhoneStateGranted = false,
            recordAudioGranted = true,
            postNotificationsGranted = true
        )

        assertFalse(state.minimumDashboardReady)
        assertTrue(state.smsReady)
        assertFalse(state.callReady)
        assertTrue(state.emailReady)
        assertTrue(state.webReady)
    }
}

