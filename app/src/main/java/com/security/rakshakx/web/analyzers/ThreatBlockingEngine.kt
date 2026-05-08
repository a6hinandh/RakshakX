package com.security.rakshakx.web.analyzers

import com.security.rakshakx.web.models.FraudAction
import java.util.concurrent.ConcurrentHashMap

class ThreatBlockingEngine {
    private val allowList = ConcurrentHashMap.newKeySet<String>()

    fun shouldBlock(domain: String, action: FraudAction): Boolean {
        if (allowList.contains(domain)) {
            return false
        }
        return action == FraudAction.BLOCK
    }

    fun allowDomain(domain: String) {
        allowList.add(domain.lowercase())
    }
}
