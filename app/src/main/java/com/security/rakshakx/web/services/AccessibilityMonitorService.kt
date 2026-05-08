package com.security.rakshakx.web.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.security.rakshakx.web.extractors.BrowserDataExtractor
import com.security.rakshakx.web.utils.BrowserLogger
import com.security.rakshakx.web.utils.BrowserSessionCache
import com.security.rakshakx.web.utils.VpnThreatLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AccessibilityMonitorService : AccessibilityService() {
    private val extractor = BrowserDataExtractor()
    private val threatLogger by lazy { VpnThreatLogger(this) }
    private val loggerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) {
            return
        }

        if (!isRelevantEvent(event.eventType)) {
            return
        }

        val packageName = event.packageName?.toString()
        if (!extractor.isSupportedBrowser(packageName)) {
            return
        }

        val root = rootInActiveWindow ?: return
        val session = extractor.extractSession(root, packageName.orEmpty())
        BrowserSessionCache.update(session)
        BrowserLogger.logSession(session)
        loggerScope.launch {
            try {
                threatLogger.logBrowserSession(session)
            } catch (e: Exception) {
                // Prevent logging failures from crashing the accessibility service
                android.util.Log.e("RakshakX", "Failed to log browser session", e)
            }
        }
    }

    override fun onDestroy() {
        loggerScope.cancel()
        super.onDestroy()
    }

    override fun onInterrupt() {
        // No-op for phase 1.
    }

    private fun isRelevantEvent(eventType: Int): Boolean {
        return eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
            eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
    }
}
