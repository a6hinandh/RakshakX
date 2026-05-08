package com.security.rakshakx.web.utils

import com.security.rakshakx.web.models.BrowserSessionData
import java.util.concurrent.ConcurrentHashMap

object BrowserSessionCache {
    private val sessions = ConcurrentHashMap<String, BrowserSessionData>()
    @Volatile
    private var lastSession: BrowserSessionData? = null

    fun update(session: BrowserSessionData) {
        sessions[session.browserPackage] = session
        lastSession = session
    }

    fun latestFor(browserPackage: String): BrowserSessionData? {
        return sessions[browserPackage]
    }

    fun latest(): BrowserSessionData? {
        return lastSession
    }
}
