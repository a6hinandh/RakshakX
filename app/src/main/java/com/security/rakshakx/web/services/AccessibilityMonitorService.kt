package com.security.rakshakx.web.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.security.rakshakx.web.extractors.BrowserDataExtractor
import com.security.rakshakx.web.utils.BrowserLogger

class AccessibilityMonitorService : AccessibilityService() {
    private val extractor = BrowserDataExtractor()

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
        BrowserLogger.logSession(session)
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
