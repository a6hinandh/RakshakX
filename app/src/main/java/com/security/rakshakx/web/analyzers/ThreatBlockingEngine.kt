package com.security.rakshakx.web.analyzers

import com.security.rakshakx.web.models.ThreatAction
import com.security.rakshakx.web.models.ThreatAssessment
import java.util.concurrent.ConcurrentHashMap

class ThreatBlockingEngine {
    private val allowList = ConcurrentHashMap.newKeySet<String>()

    fun shouldBlock(assessment: ThreatAssessment): Boolean {
        if (allowList.contains(assessment.domain)) {
            return false
        }
        return assessment.action == ThreatAction.BLOCK
    }

    fun allowDomain(domain: String) {
        allowList.add(domain.lowercase())
    }
}
